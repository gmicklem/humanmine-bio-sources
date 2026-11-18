# Test fixture cut from the current Human Protein Atlas download

`normal_ihc_data.tsv` is the header plus the first 8 rows of each of three genes (PAX6 ENSG00000007372, CYP2D6
ENSG00000100197, TSPAN6 ENSG00000000003) of the upstream file, unaltered, tab separated.

- Source: https://www.proteinatlas.org/download/tsv/normal_ihc_data.tsv.zip (the old `normal_tissue.tsv.zip` is no longer served)
- Downloaded: 2026-11-18; the zip was 5,617,166 bytes, sha256 `d53cc1d677892359489528fd9ac733656a23f19106b0c975bd84b61b56d99846`
  (the file does not state the HPA release)
- Columns: Gene, Gene name, Tissue, IHC tissue name, Cell type, Level, Reliability
- Licence: Human Protein Atlas data are CC BY-SA 4.0 (https://www.proteinatlas.org/about/licence). The few rows here are kept as
  a test fixture under the same licence; attribution: The Human Protein Atlas, https://www.proteinatlas.org.
