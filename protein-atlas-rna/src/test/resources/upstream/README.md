# Test fixture cut from the current Human Protein Atlas download

`rna_tissue_consensus.tsv` is the header plus the first 8 rows of each of four genes (PAX6 ENSG00000007372, CYP2D6
ENSG00000100197, TSPAN6 ENSG00000000003, TNMD ENSG00000000005) of the upstream file, unaltered, tab separated.

- Source: https://www.proteinatlas.org/download/tsv/rna_tissue_consensus.tsv.zip (the old `rna_tissue.tsv.zip` is no longer served)
- Downloaded: 2026-11-18; the zip was 5,293,680 bytes, sha256 `1238e89fb1094c031db0b44cba9ed97e69f2465bdcc9065d13771fd05414108b`
  (the file does not state the HPA release)
- Columns: Gene, Gene name, Tissue, nTPM
- Licence: Human Protein Atlas data are CC BY-SA 4.0 (https://www.proteinatlas.org/about/licence). The few rows here are kept as
  a test fixture under the same licence; attribution: The Human Protein Atlas, https://www.proteinatlas.org.
