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
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;
import org.intermine.xml.full.FullParser;

public class ProteinAtlasRNAConverterTest extends ItemsTestCase
{
    private final String fileName = "rna_tissue.tsv";

    public ProteinAtlasRNAConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        BioFileConverter converter = new ProteinAtlasRNAConverter(itemWriter,
                                                                   Model.getInstanceByName("genomic"));

        File testFile = new File(getClass().getClassLoader()
                .getResource(fileName).toURI());

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream(fileName));

        converter.setCurrentFile(testFile);
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "protein-atlas-rna-tgt.xml");

        assertEquals(readItemSet("ProteinAtlasRNAConverterTest-tgt.xml"), itemWriter.getItems());
    }

    /**
     * A cut of the current upstream https://www.proteinatlas.org/download/tsv/rna_tissue_consensus.tsv.zip
     * (see src/test/resources/upstream/README.md): Gene, Gene name, Tissue, nTPM.
     */
    public void testProcessConsensusFile() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        BioFileConverter converter = new ProteinAtlasRNAConverter(itemWriter,
                Model.getInstanceByName("genomic"));
        File file = new File(getClass().getClassLoader()
                .getResource("upstream/rna_tissue_consensus.tsv").toURI());
        converter.setCurrentFile(file);
        converter.process(new FileReader(file));
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
        // 4 genes with 8 tissues each
        assertEquals(32, results.size());
        assertEquals(4, genes.size());

        // PAX6 in amygdala: 16.5 nTPM
        org.intermine.xml.full.Item found = null;
        for (org.intermine.xml.full.Item r : results) {
            if ("ENSG00000007372".equals(genes.get(r.getReference("gene").getRefId()))
                    && "amygdala".equals(r.getAttribute("tissue").getValue())) {
                found = r;
            }
        }
        assertNotNull("no PAX6 amygdala result", found);
        assertEquals("16.5", found.getAttribute("expressionScore").getValue());
        assertEquals("TPM", found.getAttribute("expressionType").getValue());
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