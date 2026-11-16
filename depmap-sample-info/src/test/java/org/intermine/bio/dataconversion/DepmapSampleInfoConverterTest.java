package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.model.fulldata.Item;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.metadata.Model;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

public class DepmapSampleInfoConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    DepmapSampleInfoConverter converter;
    MockItemWriter itemWriter;

    public DepmapSampleInfoConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new DepmapSampleInfoConverter(itemWriter, model);
    }

    public void testProcess() throws Exception {
        File tmp = new File(getClass().getClassLoader().getResource("sample_info.csv").toURI());
        File dataDirectory = tmp.getParentFile();

        converter.process(dataDirectory);
        converter.close();

        // uncomment to write out a new target items file: it goes to the build directory, not into
        // the source tree (copy it to src/test/resources/DepmapSampleInfoConverterTest-tgt.xml once
        // the items have been read by eye)
        //writeItemsFile(itemWriter.getItems(), "build/depmap-sample-info-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("DepmapSampleInfoConverterTest-tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
