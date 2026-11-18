# Test fixtures cut from the current Expression Atlas files of E-MTAB-513 (Illumina Body Map)

`E-MTAB-513-fpkms.tsv` and `E-MTAB-513-tpms.tsv` are the header plus the rows of four genes (PAX6 ENSG00000007372, CYP2D6
ENSG00000100197, TSPAN6 ENSG00000000003, TNMD ENSG00000000005) of the upstream files, unaltered, tab separated.

- Source: https://ftp.ebi.ac.uk/pub/databases/microarray/data/atlas/experiments/E-MTAB-513/E-MTAB-513-fpkms.tsv and `E-MTAB-513-tpms.tsv`
  (both last modified upstream 2021-12-14)
- Downloaded: 2026-11-18. fpkms: 12,937,805 bytes, sha256 `cb3744fa7e5021089bff0be98b760018b60a2e1e1be1502a077ef084b6d51b01`;
  tpms: 12,909,800 bytes, sha256 `48bc80e92ba67c5b53556ef0a36b9debc6ae8084f47edcdd6cdba28bb0d0d729`
- Columns: GeneID, Gene Name, then g1..g16 (assay groups); each cell has 5 comma separated values (min, lower quartile, median,
  upper quartile, max over the replicates). The experiment has one assay per group, so the five values are equal.
- The names of g1..g16 are in `E-MTAB-513-configuration.xml` of the same directory (g1 leukocyte, g2 lymph node, g3 ovary, g4 breast,
  g5 kidney, g6 liver, g7 skeletal muscle, g8 heart, g9 brain, g10 prostate, g11 lung, g12 thyroid, g13 colon, g14 testis,
  g15 adipose, g16 adrenal).
- Licence: ArrayExpress / Expression Atlas data are open under the EMBL-EBI terms of use (https://www.ebi.ac.uk/about/terms-of-use);
  the Body Map data are from E-MTAB-513 (Illumina Human Body Map 2.0).
