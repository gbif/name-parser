# name-parser resource provenance

Notes on how the data files in `name-parser/src/main/resources/nameparser/` were built.
These files live outside `src/main/resources` on purpose so they don't ship inside the
published jar.

## Epithet blacklist (`blacklist-epithets.txt`)

The list of blacklisted epithets is used by the parser (`pipeline.BlacklistedEpithets`) to
flag doubtful names. The `blacklist-test.py` script in this directory queries the current
GBIF ChecklistBank API for each epithet and reports the number of matches.

### Notes on some blacklisted epithets that still yield matches in GBIF

 - `die` `Anticharis die Isiana Pilg.` is a bad name based on `Anticharis dielsiana Pilg.`
 - `mon` `Euchroeus mon` is a bad name based on [Euchroeus mongolicus in Pensoft](https://zookeys.pensoft.net/article/4271/list/13/)

### Whitelist

Some valid names that we initially had on our epithet blacklist but removed because they do indeed exist:

 - `alle` Alle alle (Linnaeus, 1758)
 - `an` Ischnothyreus an Tong & Li, 2016
 - `be` Linta be 2004
 - `den` Agnetina den 2006
 - `far` Esox far Forsskål, 1775
 - `get` Kibenikhoria get, G. G. Simpson 1935
 - `incertae` Sigmesalia incertae (Deshayes, 1832)
 - `may` Anelosimus may Agnarsson, 2005
 - `now` Apopyllus now Platnick & Shadab, 1984
 - `nur` Diospyros nur Ritter, N. & De la Barra, N. 2016
 - `once` Heterospilus once Marsh, 2013
 - `our` Mugil our Forsskål, 1775
 - `pas` Cantabroplectus pas Struyve, 2018
 - `plus` Rubus plus L.H.Bailey
 - `qui` Willowsia qui Zhang, Chen & Deharveng, 2011
 - `that` Xerolinus that (Steiner, 2006)
 - `this` Xerolinus this (Steiner, 2006)
 - `une` Trechiama une Ueno, 2001
