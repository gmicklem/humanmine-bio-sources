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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Tests for the Illumina Body Map (E-MTAB-513) converter, on cuts of the current files of
 * https://ftp.ebi.ac.uk/pub/databases/microarray/data/atlas/experiments/E-MTAB-513/
 * (see src/test/resources/upstream/README.md).
 */
public class AtlasExpressConverterTest extends ItemsTestCase
{
    private static final Set<String> TISSUES = new HashSet<String>(java.util.Arrays.asList(
            "leukocyte", "lymph node", "ovary", "breast", "kidney", "liver", "skeletal muscle",
            "heart", "brain", "prostate", "lung", "thyroid", "colon", "testis", "adipose",
            "adrenal"));

    private MockItemWriter itemWriter;

    public AtlasExpressConverterTest(String arg) {
        super(arg);
    }

    private List<org.intermine.xml.full.Item> convert(String resource) throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        AtlasExpressConverter converter = new AtlasExpressConverter(itemWriter,
                Model.getInstanceByName("genomic"));
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("9606", "5080", Collections.singleton("ENSG00000007372"));
        converter.rslv.addResolverEntry("9606", "1565", Collections.singleton("ENSG00000100197"));
        converter.rslv.addResolverEntry("9606", "7105", Collections.singleton("ENSG00000000003"));
        converter.rslv.addResolverEntry("9606", "64102", Collections.singleton("ENSG00000000005"));
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        converter.setCurrentFile(file);
        converter.process(new FileReader(file));
        converter.close();

        List<org.intermine.xml.full.Item> results = new ArrayList<org.intermine.xml.full.Item>();
        for (org.intermine.xml.full.Item item : itemWriter.getItems()) {
            if ("AtlasExpression".equals(item.getClassName())) {
                results.add(item);
            }
        }
        return results;
    }

    private String geneOf(org.intermine.xml.full.Item expression) {
        for (org.intermine.xml.full.Item item : itemWriter.getItems()) {
            if ("Gene".equals(item.getClassName())
                    && item.getIdentifier().equals(expression.getReference("gene").getRefId())) {
                return item.getAttribute("primaryIdentifier").getValue();
            }
        }
        return null;
    }

    private org.intermine.xml.full.Item find(List<org.intermine.xml.full.Item> results,
            String geneId, String condition) {
        for (org.intermine.xml.full.Item r : results) {
            if (geneId.equals(geneOf(r))
                    && condition.equals(r.getAttribute("condition").getValue())) {
                return r;
            }
        }
        return null;
    }

    /**
     * The current file names its columns g1..g16 (the assay groups of the experiment's configuration
     * file) and gives five values per cell (min, lower quartile, median, upper quartile, max).
     */
    public void testFpkmFile() throws Exception {
        List<org.intermine.xml.full.Item> results = convert("upstream/E-MTAB-513-fpkms.tsv");
        // 4 genes, 16 tissues
        assertEquals(64, results.size());
        Set<String> conditions = new HashSet<String>();
        for (org.intermine.xml.full.Item r : results) {
            conditions.add(r.getAttribute("condition").getValue());
        }
        assertEquals(TISSUES, conditions);

        // TSPAN6 in g10 = prostate: 15,15,15,15,15
        org.intermine.xml.full.Item found = find(results, "7105", "prostate");
        assertNotNull("no TSPAN6 prostate result", found);
        assertEquals("15", found.getAttribute("expression").getValue());
        assertEquals("FPKM value", found.getAttribute("type").getValue());
    }

    public void testTpmFile() throws Exception {
        List<org.intermine.xml.full.Item> results = convert("upstream/E-MTAB-513-tpms.tsv");
        assertEquals(64, results.size());
        // TSPAN6 in g10 = prostate: 58,58,58,58,58
        org.intermine.xml.full.Item found = find(results, "7105", "prostate");
        assertNotNull("no TSPAN6 prostate result", found);
        assertEquals("58", found.getAttribute("expression").getValue());
        assertEquals("TPM value", found.getAttribute("type").getValue());
    }

    /** The data set carries the licence Expression Atlas states (CC BY 4.0). */
    public void testDataSetLicence() throws Exception {
        convert("upstream/E-MTAB-513-fpkms.tsv");
        int dataSets = 0;
        for (org.intermine.xml.full.Item item : itemWriter.getItems()) {
            if ("DataSet".equals(item.getClassName())) {
                dataSets++;
                assertEquals("https://creativecommons.org/licenses/by/4.0/",
                        item.getAttribute("licence").getValue());
            }
        }
        assertEquals(1, dataSets);
    }
}
