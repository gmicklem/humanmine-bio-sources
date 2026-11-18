Fixtures for HpoConverterTest, cut from the current Human Phenotype Ontology release
files (release v2026-09-01, phenotype.hpoa version 2026-09-02, fetched 18 November 2026):

  genes_to_phenotype.txt  from https://purl.obolibrary.org/obo/hp/hpoa/genes_to_phenotype.txt
                          (full file sha256 507a17bff9c49e6329fbd88b1f91734fa7e9fdea5c06304044c0892eb6ab248c)
  phenotype.hpoa          from https://purl.obolibrary.org/obo/hp/hpoa/phenotype.hpoa
                          (full file sha256 e89aa39c8f97bf5a52c5d160f9250a603681d630ade8ec2679d84ac5aece1f72)

Both keep the header lines of the original and a few rows (genes PAX6 5080, CYP2D6 1565,
NAT2 10 and RIPK4 54101; diseases OMIM:106210 aniridia, OMIM:604229, OMIM:608902,
OMIM:243400, ORPHA:2334, ORPHA:1401 with two NOT rows, and DECIPHER:16). The rows are
unchanged. Every NOT row in the current phenotype.hpoa belongs to an ORPHA disease.

mim/mimTitles.txt is NOT OMIM data. It is a synthetic file in the layout of OMIM's
mimTitles.txt with invented titles, so that the converter's disease/gene filter can be
tested. OMIM data is licensed and must not be used as a fixture.

Licence of the HPO files: the Human Phenotype Ontology and its annotations may be reused
with attribution and without altering the terms of use, see https://hpo.jax.org/app/license.
Source: Human Phenotype Ontology, https://hpo.jax.org.

The legacy directory next to this one holds a few invented rows in the retired HPO file
formats (phenotype_annotation.tab, phenotype_annotation_negated.tab, nine column
genes_to_phenotype.txt) taken in layout from the old fixtures in this repository.
