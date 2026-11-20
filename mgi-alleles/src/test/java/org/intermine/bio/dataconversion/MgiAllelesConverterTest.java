package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2026 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Tests for MgiAllelesConverter against rows cut from the MGI reports of 20 Nov 2026 (the
 * layouts described at https://www.informatics.jax.org/downloads/reports/index.html#pheno).
 */
public class MgiAllelesConverterTest extends TestCase
{
    private Model model = Model.getInstanceByName("genomic");
    private MgiAllelesConverter converter;
    private MockItemWriter itemWriter;

    public MgiAllelesConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new MgiAllelesConverter(itemWriter, model);
    }

    private void processResource(String file) throws Exception {
        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream(file), "UTF-8");
        converter.setCurrentFile(new File(file));
        converter.process(reader);
    }

    private Map<String, org.intermine.xml.full.Item> byIdentifier() {
        Map<String, org.intermine.xml.full.Item> items =
                new HashMap<String, org.intermine.xml.full.Item>();
        for (org.intermine.xml.full.Item item : itemWriter.getItems()) {
            items.put(item.getIdentifier(), item);
        }
        return items;
    }

    private org.intermine.xml.full.Item find(String className, String attribute, String value) {
        for (org.intermine.xml.full.Item item : itemWriter.getItems()) {
            if (className.equals(item.getClassName()) && item.getAttribute(attribute) != null
                    && value.equals(item.getAttribute(attribute).getValue())) {
                return item;
            }
        }
        fail("no " + className + " with " + attribute + " = " + value);
        return null;
    }

    private Set<String> termIds(org.intermine.xml.full.Item item, String collection) {
        Map<String, org.intermine.xml.full.Item> items = byIdentifier();
        Set<String> ids = new HashSet<String>();
        if (item.getCollection(collection) != null) {
            for (String ref : item.getCollection(collection).getRefIds()) {
                ids.add(items.get(ref).getAttribute("identifier").getValue());
            }
        }
        return ids;
    }

    private Set<String> set(String... values) {
        Set<String> s = new HashSet<String>();
        for (String v : values) {
            s.add(v);
        }
        return s;
    }

    public void testPhenotypicAlleles() throws Exception {
        processResource("MGI_PhenotypicAllele.rpt");
        converter.close();

        org.intermine.xml.full.Item komp = find("Allele", "symbol",
                "1110059G10Rik<tm1a(KOMP)Wtsi>");
        assertEquals("MGI:4419470", komp.getAttribute("primaryIdentifier").getValue());
        assertEquals("Targeted, Conditional ready|Null/knockout|Reporter",
                komp.getAttribute("type").getValue());
        assertEquals(set("MP:0005390"), termIds(komp, "highLevelPhenotypeTerms"));
        assertEquals("MGI:1913452", byIdentifier().get(komp.getReference("gene").getRefId())
                .getAttribute("primaryIdentifier").getValue());

        org.intermine.xml.full.Item aex = find("Allele", "symbol", "12aex");
        assertEquals(set("MP:0003631", "MP:0010768"), termIds(aex, "highLevelPhenotypeTerms"));
        assertEquals("15755804", byIdentifier().get(aex.getReference("publication").getRefId())
                .getAttribute("pubMedId").getValue());

        // no phenotype terms in the file: the Ensembl id must not be read as one
        org.intermine.xml.full.Item none = find("Allele", "symbol",
                "0610030E20Rik<em1(IMPC)Mbp>");
        assertTrue(termIds(none, "highLevelPhenotypeTerms").isEmpty());
        for (org.intermine.xml.full.Item item : itemWriter.getItems()) {
            assertFalse("Ensembl id loaded as a phenotype term",
                    "MammalianPhenotypeTerm".equals(item.getClassName())
                    && item.getAttribute("identifier").getValue().startsWith("ENSMUSG"));
        }
    }

    public void testQtlAlleles() throws Exception {
        processResource("MGI_QTLAllele.rpt");
        converter.close();

        org.intermine.xml.full.Item aabpr = find("Allele", "symbol", "Aabpr<NZB/Slc>");
        assertEquals(set("MP:0005384", "MP:0005387", "MP:0005397"),
                termIds(aabpr, "highLevelPhenotypeTerms"));
        assertEquals("QTL", aabpr.getAttribute("type").getValue());
        assertEquals("14695357", byIdentifier().get(aabpr.getReference("publication").getRefId())
                .getAttribute("pubMedId").getValue());

        org.intermine.xml.full.Item aaj1 = find("Allele", "symbol", "Aaj1<A/J>");
        assertEquals(set("MP:0005386"), termIds(aaj1, "highLevelPhenotypeTerms"));
    }

    public void testGenotypes() throws Exception {
        processResource("MGI_PhenoGenoMP.rpt");
        converter.close();

        org.intermine.xml.full.Item gli3 = find("Genotype", "name", "Gli3<Xt-J>/Gli3<Xt-J>");
        assertEquals("homozygote", gli3.getAttribute("zygosity").getValue());
        assertEquals(set("MP:0000562"), termIds(gli3, "phenotypeTerms"));

        org.intermine.xml.full.Item kdr = find("Genotype", "name", "Kdr<tm1Jrt>/Kdr<+>");
        assertEquals("heterozygote", kdr.getAttribute("zygosity").getValue());

        // two markers (MGI:108359|MGI:88180): only the marker column says so
        org.intermine.xml.full.Item alx4 = find("Genotype", "name",
                "Alx4<tm1Rwi>/Alx4<+>,Bmp4<tm1Blh>/Bmp4<+>");
        assertEquals("complex: > 1 genome feature", alx4.getAttribute("zygosity").getValue());

        org.intermine.xml.full.Item wnt = find("Genotype", "name",
                "Wnt1<tm1Brd>/Wnt1<tm1Brd>,Wnt3a<tm1Amc>/Wnt3a<tm1Amc>");
        assertEquals("complex: > 1 genome feature", wnt.getAttribute("zygosity").getValue());
        assertEquals(set("MP:0000737", "MP:0002260"), termIds(wnt, "phenotypeTerms"));
    }

    private void assertLayoutRejected(String file, String content, String... expectedInMessage)
        throws Exception {
        converter.setCurrentFile(new File(file));
        try {
            converter.process(new StringReader(content));
            fail("a layout MGI no longer or not yet uses must be rejected: " + file);
        } catch (RuntimeException e) {
            for (String s : expectedInMessage) {
                assertTrue("message '" + e.getMessage() + "' lacks '" + s + "'",
                        e.getMessage().contains(s));
            }
        }
    }

    public void testUnknownColumnLayoutIsRejected() throws Exception {
        // the 12-column layout from before MGI added the Marker Ensembl ID column
        assertLayoutRejected("MGI_PhenotypicAllele.rpt",
                "MGI:1\ta<x>\tname\tTargeted\tNull/knockout\t123\tMGI:2\ta\tNM_1\tMP:0005390\t\tname\n",
                "MGI_PhenotypicAllele.rpt", "13 columns", "Marker Ensembl ID");
        // 13 columns, but an Ensembl id where the phenotype ids should be
        assertLayoutRejected("MGI_PhenotypicAllele.rpt",
                "MGI:1\ta<x>\tname\tTargeted\tNull/knockout\t\tMGI:2\ta\tNM_1\tMP:0005390\tENSMUSG00000032551\t\tname\n",
                "MGI_PhenotypicAllele.rpt", "High-level Mammalian Phenotype ID");
        assertLayoutRejected("MGI_QTLAllele.rpt",
                "MGI:1\ta<x>\tname\tQTL\t123\tMGI:2\ta\t\t\t1\t10\t20\tGRCm39\n"
                + "MGI:1\ta<x>\tname\tQTL\t123\tMGI:2\ta\t\tENSMUSG00000032551\t1\t10\t20\tGRCm39\tMP:0005390,x\n",
                "MGI_QTLAllele.rpt", "14 columns");
        // PubMed ids where the marker ids should be (the layout the converter was written for)
        assertLayoutRejected("MGI_PhenoGenoMP.rpt",
                "a<x>/a<x>\ta<x>\tinvolves: C3H\tMP:0000562\tMGI:95729\t8387379|9006072\tMGI:2166944\n",
                "MGI_PhenoGenoMP.rpt", "MGI Marker Accession IDs");
        assertLayoutRejected("MGI_PhenoGenoMP.rpt",
                "a<x>/a<x>\ta<x>\tinvolves: C3H\tMP:0000562\t8387379\n",
                "MGI_PhenoGenoMP.rpt", "7 columns");
    }
}
