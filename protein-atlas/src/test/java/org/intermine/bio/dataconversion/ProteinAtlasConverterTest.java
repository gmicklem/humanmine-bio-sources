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
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;
import org.intermine.xml.full.FullParser;

public class ProteinAtlasConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public ProteinAtlasConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        String input = "Gene\tSymbol\tTissue\tCell type\tLevel\tReliability" + ENDL
            + "ENSG00000000003\tTSPAN6\tadrenal gland\tglandular cells\tNegative\tSupportive" + ENDL
            + "ENSG00000000003\tTSPAN6\tappendix\tglandular cells\tModerate\tSupportive" + ENDL;


        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        BioFileConverter converter = new ProteinAtlasConverter(itemWriter,
                                                                   Model.getInstanceByName("genomic"));
        converter.setCurrentFile(new File("normal_tissue.tsv"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "protein-atlas-tgt.xml");

        assertEquals(readItemSet("ProteinAtlasConverterTest.xml"), itemWriter.getItems());
    }

    /**
     * A cut of the current upstream https://www.proteinatlas.org/download/tsv/normal_ihc_data.tsv.zip
     * (see src/test/resources/upstream/README.md): 7 columns, "IHC tissue name" sits between Tissue and
     * Cell type, and the file has a different name from the old normal_tissue.tsv.
     */
    public void testProcessCurrentUpstreamFile() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        BioFileConverter converter = new ProteinAtlasConverter(itemWriter,
                Model.getInstanceByName("genomic"));
        File file = new File(getClass().getClassLoader()
                .getResource("upstream/normal_ihc_data.tsv").toURI());
        converter.setCurrentFile(file);
        converter.process(new FileReader(file));
        converter.close();

        Map<String, String> genes = new HashMap<String, String>();
        List<org.intermine.xml.full.Item> expressions = new ArrayList<org.intermine.xml.full.Item>();
        for (org.intermine.xml.full.Item item : itemWriter.getItems()) {
            if ("Gene".equals(item.getClassName())) {
                genes.put(item.getIdentifier(), item.getAttribute("primaryIdentifier").getValue());
            } else if ("ProteinAtlasExpression".equals(item.getClassName())) {
                expressions.add(item);
            }
        }
        // 3 genes with 8 rows each in the fixture (ENSG00000000005 is not in the IHC file)
        assertEquals(24, expressions.size());
        assertEquals(new HashSet<String>(Arrays.asList("ENSG00000007372", "ENSG00000100197",
                "ENSG00000000003")), new HashSet<String>(genes.values()));

        // PAX6, appendix, endocrine cells: High / Enhanced
        org.intermine.xml.full.Item found = null;
        for (org.intermine.xml.full.Item e : expressions) {
            String gene = genes.get(e.getReference("gene").getRefId());
            if ("ENSG00000007372".equals(gene)
                    && "endocrine cells".equals(e.getAttribute("cellType").getValue())) {
                found = e;
            }
        }
        assertNotNull("no PAX6 endocrine cells expression", found);
        assertEquals("High", found.getAttribute("level").getValue());
        assertEquals("Enhanced", found.getAttribute("reliability").getValue());
    }

    @SuppressWarnings("rawtypes")
    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("ProteinAtlasConverterTest.xml"));
    }

    // we don't parse the XML any more.
//    public void testXMLParsing() throws Exception {
//        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
//        BioFileConverter converter = new ProteinAtlasConverter(itemWriter,
//                                                                   Model.getInstanceByName("genomic"));
//
//        File testFile = new File(getClass().getClassLoader()
//                .getResource("proteinatlas.xml").toURI());
//        converter.setCurrentFile(testFile);
//        converter.process(null);
//        converter.close();
//
//        // uncomment to write out a new target items file
//        // writeItemsFile(itemWriter.getItems(), "protein-atlas-tgt.xml");
//
//        assertEquals(readItemSet("ProteinAtlasConverterXMLParsingTest.xml"), itemWriter.getItems());
//    }
}