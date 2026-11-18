package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class GtexConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    GtexConverter converter;
    MockItemWriter itemWriter;
    private final String targetFile = "GtexConverterTest_tgt.xml";

    public GtexConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new GtexConverter(itemWriter, model);
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("9606", "123", Collections.singleton("ENSG00000225630"));
    }

    public void testProcess() throws Exception {

        File tmp = new File(getClass().getClassLoader().getResource(targetFile).toURI());
        File datadir = tmp.getParentFile();
        converter.process(datadir);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "gtex-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet(targetFile);

        assertEquals(expected, itemWriter.getItems());
    }

    /**
     * A cut of the GTEx v8 median TPM file (see src/test/resources/v8/README.md), on its own in a
     * directory: its header row starts with "Name", not "gene_id" as in v7.
     */
    public void testProcessV8MedianFile() throws Exception {
        File datadir = new File(getClass().getClassLoader().getResource(
                "v8/GTEx_Analysis_2017-06-05_v8_RNASeQCv1.1.9_gene_median_tpm.gct").toURI())
                .getParentFile();
        converter.rslv.addResolverEntry("9606", "5080", Collections.singleton("ENSG00000007372"));
        converter.rslv.addResolverEntry("9606", "1565", Collections.singleton("ENSG00000100197"));
        converter.rslv.addResolverEntry("9606", "7105", Collections.singleton("ENSG00000000003"));
        converter.rslv.addResolverEntry("9606", "64102", Collections.singleton("ENSG00000000005"));
        converter.process(datadir);
        converter.close();

        Map<String, String> genes = new HashMap<String, String>();
        List<org.intermine.xml.full.Item> results = new ArrayList<org.intermine.xml.full.Item>();
        for (org.intermine.xml.full.Item item : itemWriter.getItems()) {
            if ("Gene".equals(item.getClassName())) {
                genes.put(item.getIdentifier(), item.getAttribute("primaryIdentifier").getValue());
            } else if ("RNASeqResult".equals(item.getClassName())) {
                results.add(item);
            }
        }
        // 4 genes, 54 tissues
        assertEquals(4, genes.size());
        assertEquals(216, results.size());

        // PAX6 in the cerebellar hemisphere
        org.intermine.xml.full.Item found = null;
        for (org.intermine.xml.full.Item r : results) {
            if ("5080".equals(genes.get(r.getReference("gene").getRefId()))
                    && "Brain - Cerebellar Hemisphere".equals(r.getAttribute("tissue").getValue())) {
                found = r;
            }
        }
        assertNotNull("no PAX6 cerebellar hemisphere result", found);
        assertEquals("40.816", found.getAttribute("expressionScore").getValue());
        assertEquals("TPM", found.getAttribute("expressionType").getValue());
    }
}
