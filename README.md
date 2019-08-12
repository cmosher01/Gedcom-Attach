# Gedcom-Attach

Copyright © 2019, Christopher Alan Mosher, Shelton, Connecticut, USA, <cmosher01@gmail.com>.

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=CVSSQ2BWDCKQ2)
[![License](https://img.shields.io/github/license/cmosher01/Gedcom-Attach.svg)](https://www.gnu.org/licenses/gpl.html)

Attaches GEDCOM files together. Matches individuals based on `REFN` values and
drops duplicates (does not merge). The intended use case is when a single family
tree had been split into multiple GEDCOM files, with common individuals having
the same `REFN` values. This program then re-attaches the trees into one file.

This project is in alpha phase; it does not handle family links correctly in
every case, for duplicate individuals. It will merge two spouses, but copy
both families, resulting in duplicate `FAM` records.

Warning: the program assumes every ID is unique, even
across different GEDCOM files. In this regard, you should first run each input
GEDCOM file through [Gedcom-Uid](https://github.com/cmosher01/Gedcom-Uid).
