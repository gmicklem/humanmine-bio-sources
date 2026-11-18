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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Tests for the HPO annotation converter. The files in src/test/resources/current are cuts of
 * the current HPO release files (see the README there), those in src/test/resources/legacy
 * are small files in the retired HPO formats.
 */
public class HpoConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    HpoConverter converter;
    MockItemWriter itemWriter;

    public HpoConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new HpoConverter(itemWriter, model);
    }

    private File resource(String name) throws Exception {
        return new File(getClass().getClassLoader().getResource(name).toURI());
    }

    private List<org.intermine.xml.full.Item> items(String className) {
        List<org.intermine.xml.full.Item> results = new ArrayList<org.intermine.xml.full.Item>();
        for (org.intermine.xml.full.Item item : itemWriter.getItems()) {
            if (className.equals(item.getClassName())) {
                results.add(item);
            }
        }
        return results;
    }

    private Map<String, org.intermine.xml.full.Item> byAttribute(String className, String attr) {
        Map<String, org.intermine.xml.full.Item> results =
                new HashMap<String, org.intermine.xml.full.Item>();
        for (org.intermine.xml.full.Item item : items(className)) {
            results.put(item.getAttribute(attr).getValue(), item);
        }
        return results;
    }

    private org.intermine.xml.full.Item findAnnotation(String hpoId, String diseaseId,
            String qualifier) {
        Map<String, org.intermine.xml.full.Item> terms = byAttribute("HPOTerm", "identifier");
        Map<String, org.intermine.xml.full.Item> diseases =
                byAttribute("Disease", "primaryIdentifier");
        String termRef = terms.get(hpoId).getIdentifier();
        String diseaseRef = diseases.get(diseaseId).getIdentifier();
        for (org.intermine.xml.full.Item annotation : items("HPOAnnotation")) {
            String q = annotation.getAttribute("qualifier") == null ? ""
                    : annotation.getAttribute("qualifier").getValue();
            if (termRef.equals(annotation.getReference("hpoTerm").getRefId())
                    && q.equals(qualifier)
                    && diseases.get(diseaseId).getCollection("hpoAnnotations").getRefIds()
                            .contains(annotation.getIdentifier())) {
                return annotation;
            }
        }
        fail("no annotation " + hpoId + " " + diseaseId + " '" + qualifier + "' " + diseaseRef);
        return null;
    }

    /**
     * The current files, with a (synthetic) OMIM mimTitles.txt: only OMIM entries listed as
     * diseases and Orphanet diseases are loaded.
     */
    public void testProcessCurrentWithMimTitles() throws Exception {
        File datadir = resource("current/phenotype.hpoa").getParentFile();
        converter.setHpoDiseaseFile(resource("current/mim/mimTitles.txt").getAbsolutePath());
        converter.process(datadir);
        converter.close();

        Map<String, org.intermine.xml.full.Item> diseases =
                byAttribute("Disease", "primaryIdentifier");
        assertTrue(diseases.containsKey("OMIM:106210"));
        assertTrue(diseases.containsKey("OMIM:604229"));
        assertTrue(diseases.containsKey("OMIM:608902"));
        assertTrue(diseases.containsKey("OMIM:243400"));
        assertTrue(diseases.containsKey("ORPHANET:2334"));
        assertTrue(diseases.containsKey("ORPHANET:1401"));
        // 120200 is a gene in the synthetic mimTitles, 165550 is not in it at all
        assertFalse(diseases.containsKey("OMIM:120200"));
        assertFalse(diseases.containsKey("OMIM:165550"));
        for (String id : diseases.keySet()) {
            assertFalse(id, id.startsWith("DECIPHER") || id.startsWith("ORPHA:"));
        }

        Map<String, org.intermine.xml.full.Item> genes = byAttribute("Gene", "primaryIdentifier");
        assertEquals(4, genes.size());
        org.intermine.xml.full.Item pax6 = genes.get("5080");
        assertTrue(pax6.getCollection("diseases").getRefIds()
                .contains(diseases.get("OMIM:106210").getIdentifier()));
        assertTrue(pax6.getCollection("diseases").getRefIds()
                .contains(diseases.get("ORPHANET:2334").getIdentifier()));
        assertFalse(pax6.getCollection("diseases").getRefIds()
                .contains(diseases.get("OMIM:243400").getIdentifier()));
        assertTrue(genes.get("1565").getCollection("diseases").getRefIds()
                .contains(diseases.get("OMIM:608902").getIdentifier()));
        assertTrue(diseases.get("OMIM:106210").getCollection("genes").getRefIds()
                .contains(pax6.getIdentifier()));

        // one annotation with evidence, two PubMed references, a frequency and curators
        org.intermine.xml.full.Item aniridia = findAnnotation("HP:0000526", "OMIM:106210", "");
        assertEquals(1, aniridia.getCollection("evidences").getRefIds().size());
        String evidenceRef = aniridia.getCollection("evidences").getRefIds().get(0);
        org.intermine.xml.full.Item evidence = null;
        for (org.intermine.xml.full.Item e : items("HPOEvidence")) {
            if (evidenceRef.equals(e.getIdentifier())) {
                evidence = e;
            }
        }
        assertNotNull(evidence);
        assertEquals("PMID:16543198;PMID:7550230", evidence.getAttribute("source").getValue());
        assertEquals("19/19", evidence.getAttribute("frequencyModifier").getValue());
        assertEquals("HPO:iea;HPO:probinson", evidence.getAttribute("assignedBy").getValue());
        assertEquals(2, evidence.getCollection("publications").getRefIds().size());
        assertEquals(diseases.get("OMIM:106210").getIdentifier(),
                evidence.getReference("diseaseReference").getRefId());
        Map<String, org.intermine.xml.full.Item> codes =
                byAttribute("OntologyAnnotationEvidenceCode", "code");
        assertEquals(codes.get("PCS").getIdentifier(), evidence.getReference("code").getRefId());
        assertEquals(3, codes.size());

        // negated annotations now come in the same file, with qualifier NOT
        org.intermine.xml.full.Item negated = findAnnotation("HP:0001270", "ORPHANET:1401", "NOT");
        assertEquals("NOT", negated.getAttribute("qualifier").getValue());
        assertEquals(1, negated.getCollection("evidences").getRefIds().size());

        // the HPO terms link to diseases from the gene file
        Map<String, org.intermine.xml.full.Item> terms = byAttribute("HPOTerm", "identifier");
        assertTrue(terms.get("HP:0000526").getCollection("diseases").getRefIds()
                .contains(diseases.get("OMIM:106210").getIdentifier()));
        assertTrue(byAttribute("Ontology", "name").containsKey("Human Phenotype Ontology"));
        assertEquals(terms.get("HP:0000526").getReference("ontology").getRefId(),
                byAttribute("Ontology", "name").get("Human Phenotype Ontology").getIdentifier());
    }

    /**
     * Without mimTitles.txt every OMIM identifier in the current files is a disease.
     */
    public void testProcessCurrentWithoutMimTitles() throws Exception {
        File datadir = resource("current/phenotype.hpoa").getParentFile();
        converter.process(datadir);
        converter.close();

        Map<String, org.intermine.xml.full.Item> diseases =
                byAttribute("Disease", "primaryIdentifier");
        assertTrue(diseases.containsKey("OMIM:120200"));
        assertTrue(diseases.containsKey("OMIM:165550"));
        assertTrue(diseases.containsKey("OMIM:106210"));
        assertTrue(diseases.containsKey("ORPHANET:2334"));
        for (String id : diseases.keySet()) {
            assertFalse(id, id.startsWith("DECIPHER") || id.startsWith("ORPHA:"));
        }
        findAnnotation("HP:0001270", "ORPHANET:1401", "NOT");
    }

    /**
     * The retired HPO files (phenotype_annotation.tab, phenotype_annotation_negated.tab and the
     * nine column genes_to_phenotype.txt) are still read, with a mimTitles.txt.
     */
    public void testProcessLegacyFiles() throws Exception {
        File datadir = resource("legacy/genes_to_phenotype.txt").getParentFile();
        converter.setHpoDiseaseFile(resource("mimTitles.txt").getAbsolutePath());
        converter.process(datadir);
        converter.close();

        Map<String, org.intermine.xml.full.Item> diseases =
                byAttribute("Disease", "primaryIdentifier");
        assertEquals(2, diseases.size());
        assertTrue(diseases.containsKey("OMIM:614652"));
        assertTrue(diseases.containsKey("OMIM:125853"));
        Map<String, org.intermine.xml.full.Item> genes = byAttribute("Gene", "primaryIdentifier");
        assertEquals(1, genes.size());
        assertEquals(2, genes.get("57107").getCollection("diseases").getRefIds().size());
        findAnnotation("HP:0000007", "OMIM:614652", "");
        findAnnotation("HP:0009737", "OMIM:614652", "NOT");
    }
}
