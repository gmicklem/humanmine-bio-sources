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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang3.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * DataConverter to parse a HPO annotation file into Items.
 *
 * @author Fengyuan Hu
 */
public class HpoConverter extends BioDirectoryConverter
{

    private static final String DATASET_TITLE = "hpo-annotation";
    private static final String DATA_SOURCE_NAME = "Human Phenotype Ontology";

    private List<String> ignoreDbList = Arrays.asList("DECIPHER");

    private static final String HPOTEAM_FILE = "phenotype_annotation.tab";
    private static final String NEG_FILE = "phenotype_annotation_negated.tab";
    private static final String HPOA_FILE = "phenotype.hpoa";
    private static final String GENE_FILE = "genes_to_phenotype.txt";
    private static final String OMIM_PREFIX = "OMIM:";
    private static final String HPO_ORPHANET_PREFIX = "ORPHA:";
    private static final String ORPHANET_PREFIX = "ORPHANET:";
    private String omimFile = null;
    private static final String GENE_ENTRY = "Asterisk";
    private static final String GENE_PHENOTYPE_ENTRY = "Plus";
    private static final String OBSOLETE = "Caret";

    private Map<String, Item> diseases = new HashMap<String, Item>();
    private Map<String, Item> hpoTerms = new HashMap<String, Item>();
    private Map<String, Item> genes = new HashMap<String, Item>();
    private Set<String> omimDiseaseMasterList = new HashSet<String>();
    private Map<String, String> evidenceCodes = new HashMap<String, String>();
    private Map<MultiKey, Item> annotations = new HashMap<MultiKey, Item>();
    private Map<String, String> publications = new HashMap<String, String>();
    private String ontologyItemId = null;

    private static final String HUMAN_TAXON = "9606";
    private String organism = getOrganism(HUMAN_TAXON);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws Exception if something goes wrong
     */
    public HpoConverter(ItemWriter writer, Model model) throws Exception {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE, "https://hpo.jax.org/app/license");
    }

    @Override
    public void process(File dataDir) throws Exception {
        Map<String, File> files = readFilesInDir(dataDir);

        // current HPO releases have phenotype.hpoa (which includes the negated annotations);
        // the retired phenotype_annotation.tab and phenotype_annotation_negated.tab are still read
        boolean current = files.containsKey(HPOA_FILE);
        Set<String> missingFiles = new HashSet<String>();
        if (!files.containsKey(GENE_FILE)) {
            missingFiles.add(GENE_FILE);
        }
        if (!current) {
            for (String requiredFile : new String[] {HPOTEAM_FILE, NEG_FILE}) {
                if (!files.containsKey(requiredFile)) {
                    missingFiles.add(requiredFile);
                }
            }
        }

        if (!missingFiles.isEmpty()) {
            throw new RuntimeException("Not all required files for the HPO annotation source were "
                    + "found in: " + dataDir.getAbsolutePath() + ", was missing " + missingFiles
                    + " (or " + HPOA_FILE + " instead of the two annotation files)");
        }
        if (!current && omimFile == null) {
            throw new IllegalArgumentException("The retired HPO annotation files list OMIM genes "
                    + "as well as diseases, set hpo.disease.file to OMIM's mimTitles.txt");
        }

        ontologyItemId = storeOntology();
        if (omimFile != null) {
            processOMIMFile(new FileReader(omimFile));
        }
        processGeneFile(new FileReader(files.get(GENE_FILE)));
        if (current) {
            processHpoaFile(new FileReader(files.get(HPOA_FILE)));
        } else {
            processAnnotationFile(new FileReader(files.get(HPOTEAM_FILE)));
            processAnnotationFile(new FileReader(files.get(NEG_FILE)));
        }
    }

    /**
     * HPO lists all OMIM IDs whether they are genes or diseases in the retired files. There is no
     * way to differeniate the two. So instead we use the OMIM file which does label diseases and
     * genes correctly. The current phenotype.hpoa and genes_to_phenotype.txt only list diseases,
     * so this file is optional for them: if it is set, OMIM entries not listed as diseases in it
     * are still skipped.
     *
     * @param fileName name of OMIM file that lists all diseases and OMIM identifiers
     */
    public void setHpoDiseaseFile(String fileName) {
        if (StringUtils.isNotEmpty(fileName)) {
            this.omimFile = fileName;
        }
    }

    private static Map<String, File> readFilesInDir(File dir) {
        Map<String, File> files = new HashMap<String, File>();
        for (File file : dir.listFiles()) {
            files.put(file.getName(), file);
        }
        return files;
    }

    /**
     * @param reader file reader
     * @throws IOException if can't read file
     * @throws ObjectStoreException if can't store to db
     */
    protected void processGeneFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<?> lineIter = FormattedTextParser.
                parseTabDelimitedReader(new BufferedReader(reader));

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            // the retired file has a comment line first, the current one a column header
            if (line[0].startsWith("#") || "ncbi_gene_id".equals(line[0])) {
                continue;
            }

            // gene id, symbol, HPO id, HPO name, ...; the disease is the last column (the ninth
            // in the retired file, the sixth since 2023)
            String diseaseId = line[line.length - 1];
            String identifier = line[0];
            String hpoId = line[2];

            Item disease = getDisease(diseaseId);
            if (disease == null) {
                // whoops this is a gene. genes have OMIM IDs too. ignore.
                continue;
            }
            Item gene = getGene(identifier);
            gene.addToCollection("diseases", disease);
            disease.addToCollection("genes", gene);
            Item term = getTerm(hpoId);
            term.addToCollection("diseases", disease);
        }
    }

    private Item getDisease(String diseaseId) {
        String identifier = null;
        if (diseaseId.startsWith(OMIM_PREFIX)) {
            // only create diseases that are really diseases not genes (if we can tell).
            if (omimFile == null || omimDiseaseMasterList.contains(diseaseId)) {
                identifier = diseaseId;
            }
        } else if (diseaseId.startsWith(HPO_ORPHANET_PREFIX)) {
            // HPO's ORPHA:166024 is ORPHANET:166024 in the orphanet source
            identifier = ORPHANET_PREFIX + diseaseId.substring(HPO_ORPHANET_PREFIX.length());
        }
        if (identifier == null) {
            return null;
        }
        Item item = diseases.get(identifier);
        if (item == null) {
            item = createItem("Disease");
            item.setAttribute("primaryIdentifier", identifier);
            diseases.put(identifier, item);
        }
        return item;
    }

    private Item getTerm(String hpoTerm) {
        Item item = hpoTerms.get(hpoTerm);
        if (item == null) {
            item = createItem("HPOTerm");
            item.setAttribute("identifier", hpoTerm);
            item.setReference("ontology", ontologyItemId);
            hpoTerms.put(hpoTerm, item);
        }
        return item;
    }

    private Item getGene(String identifier) throws ObjectStoreException {
        Item item = genes.get(identifier);
        if (item == null) {
            item = createItem("Gene");
            item.setAttribute("primaryIdentifier", identifier);
            item.setReference("organism", organism);
            genes.put(identifier, item);
        }
        return item;
    }

    /**
     * @param reader file reader
     * @throws IOException if can't read file
     * @throws ObjectStoreException if can't store to db
     */
    protected void processAnnotationFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] array = lineIter.next();
            // HPO Annotation File Format:
            // http://human-phenotype-ontology.github.io/documentation.html
            if (array.length < 9) {
                throw new IllegalArgumentException("Not enough elements (should be > 8 not "
                        + array.length + ")");
            }

            // e.g. OMIM
            String db = array[0];
            if (ignoreDbList.contains(db)) {
                continue;
            }

            // e.g. OMIM:100200
            String dbId = db + ":" + array[1];
            // e.g. NOT
            String qualifier = array[3];
            // HP:0000028
            String hpoIdentifier = array[4];
            //  PMID:17088400 OR OMIM:100050
            String dbRef = array[5];
            // e.g. IEA
            String evidenceCode = array[6];
            // e.g RARE
            String frequency = array[8];
            // HPO:curators
            String assignedBy = array[13];

            storeAnnotation(dbId, qualifier, hpoIdentifier, dbRef, evidenceCode, frequency,
                    assignedBy);
        }
    }

    /**
     * Read the current phenotype.hpoa: tab delimited, comment lines start with #, then a column
     * header line, then database_id (e.g. OMIM:100200, ORPHA:166024, DECIPHER:16), disease_name,
     * qualifier (NOT or empty), hpo_id, reference, evidence, onset, frequency, sex, modifier,
     * aspect and biocuration (e.g. HPO:probinson[2021-03-23]). Negated annotations are in this
     * file. Disease name, onset, sex, modifier and aspect are not stored.
     *
     * @param reader file reader
     * @throws IOException if can't read file
     * @throws ObjectStoreException if can't store to db
     */
    protected void processHpoaFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(
                new BufferedReader(reader));
        while (lineIter.hasNext()) {
            String[] array = lineIter.next();
            if (array[0].startsWith("#") || "database_id".equals(array[0])) {
                continue;
            }
            if (array.length < 12) {
                throw new IllegalArgumentException("Not enough elements (should be 12 not "
                        + array.length + ")");
            }
            // curators with their dates, the dates are dropped as they were in the retired files
            Set<String> curators = new LinkedHashSet<String>();
            for (String curator : array[11].split(";")) {
                String name = curator.replaceAll("\\[.*\\]", "").trim();
                if (!name.isEmpty()) {
                    curators.add(name);
                }
            }
            storeAnnotation(array[0], array[2], array[3], array[4], array[5], array[7],
                    StringUtils.join(curators, ";"));
        }
    }

    private void storeAnnotation(String dbId, String qualifier, String hpoIdentifier,
            String dbRef, String evidenceCode, String frequency, String assignedBy)
        throws ObjectStoreException {
        String evidenceCodeRefId = getEvidenceCode(evidenceCode);

        Item disease = getDisease(dbId);
        if (disease == null) {
            // whoops this is a gene. genes have OMIM IDs too. ignore.
            return;
        }
        Item evidence = createItem("HPOEvidence");
        evidence.setReference("diseaseReference", disease);
        if (dbRef.isEmpty()) {
            dbRef = dbId;
        }
        evidence.setAttribute("source", dbRef);
        if (StringUtils.isNotEmpty(dbRef)) {
            String[] bits = dbRef.split(";");
            for (String bit : bits) {
                if (bit.toUpperCase().startsWith("PMID")) {
                    String refId = getPublication(bit);
                    if (refId != null) {
                        evidence.addToCollection("publications", refId);
                    }
                }
            }
        }

        evidence.setReference("code", evidenceCodeRefId);
        if (!frequency.isEmpty()) {
            evidence.setAttribute("frequencyModifier", frequency);
        }
        if (!assignedBy.isEmpty()) {
            evidence.setAttribute("assignedBy", assignedBy);
        }
        store(evidence);

        Item annotation = getAnnotation(hpoIdentifier, dbId, qualifier);
        annotation.addToCollection("evidences", evidence);
        disease.addToCollection("hpoAnnotations", annotation);
    }

    private Item getAnnotation(String hpoId, String diseaseId, String qualifier)
        throws ObjectStoreException {
        MultiKey key = new MultiKey(hpoId, diseaseId, qualifier);
        Item annotation = annotations.get(key);
        if (annotation == null) {
            annotation = createItem("HPOAnnotation");
            Item hpoTerm = getTerm(hpoId);
            annotation.setReference("hpoTerm", hpoTerm);
            hpoTerm.addToCollection("hpoAnnotations", annotation);
            if (!qualifier.isEmpty()) {
                annotation.setAttribute("qualifier", qualifier);
            }
            annotations.put(key, annotation);
        }
        return annotation;
    }

    @Override
    public void close() throws Exception {
        store(hpoTerms.values());
        store(diseases.values());
        store(genes.values());
        store(annotations.values());
        super.close();
    }

    private String storeOntology() throws ObjectStoreException {
        Item item = createItem("Ontology");
        item.setAttribute("name", "Human Phenotype Ontology");
        item.setAttribute("url", "http://www.human-phenotype-ontology.org");
        store(item);
        return item.getIdentifier();
    }

    private String getEvidenceCode(String code) throws ObjectStoreException {
        String refId = evidenceCodes.get(code);
        if (refId == null) {
            Item item = createItem("OntologyAnnotationEvidenceCode");
            item.setAttribute("code", code);
            refId = item.getIdentifier();
            evidenceCodes.put(code, refId);
            store(item);
        }
        return refId;
    }

    private String getPublication(String ref) throws ObjectStoreException {
        String pubMedId = ref.trim().substring(5);
        if (StringUtil.allDigits(pubMedId)) {
            String pubItemId = publications.get(pubMedId);
            if (pubItemId == null) {
                Item item = createItem("Publication");
                item.setAttribute("pubMedId", pubMedId);
                pubItemId = item.getIdentifier();
                publications.put(pubMedId, pubItemId);
                store(item);
            }
            return pubItemId;
        }
        return null;
    }

    /**
     * @param reader file reader
     * @throws IOException if can't read file
     */
    protected void processOMIMFile(Reader reader) throws IOException {
        Iterator<?> lineIter = FormattedTextParser.
                parseTabDelimitedReader(new BufferedReader(reader));

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String prefix = line[0].trim();
            String mimId = line[1];
            // String preferredTitles = line[2]; - don't need names

            // skip header AND genes
            if (GENE_ENTRY.equals(prefix) || GENE_PHENOTYPE_ENTRY.equals(prefix)
                    || OBSOLETE.equals(prefix)) {
                continue;
            }

            omimDiseaseMasterList.add("OMIM:" + mimId);
        }
    }
}
