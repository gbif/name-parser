# latin-endings.txt
The suffices were generated querying the genus names in checklistbank. All endings with 4 chars that existed in at least 100 different genus names were added.
The query used was:

   select substring(genus from '....$') as suffix from (select distinct genus_or_above as genus from name) as genera group by suffix having count(*) > 100;

To sort the suffices by their ending linux provides a nice tool:

   $ cat latin-endings.txt | rev | sort | rev > latin-endings.txt2




# Epithet Blacklist
The list of blacklisted epithets is used by the parser to flag doubtful names.
A little python script blacklist-test.py can be used to query the current GBIF ChecklistBank API for each epithet and report number of matches.

## Notes on some blacklisted epithets that still yield matches in GBIF

 - die: 'Anticharis die Isiana Pilg.' is a bad name based on 'Anticharis dielsiana Pilg.'
 - mon: 'Euchroeus mon' is a bad name based on [Euchroeus mongolicus in Pensoft](https://zookeys.pensoft.net/article/4271/list/13/)

 ## Whitelist
 Some valid names that we initially had on our epithet blacklist but removed them because they do indeed exist:

'alle' Alle alle (Linnaeus, 1758)
'an' Ischnothyreus an Tong & Li, 2016
'be' Linta be 2004
'den' Agnetina den 2006
'far' Esox far Forsskål, 1775
'get' Kibenikhoria get, G. G. Simpson 1935
'may' Anelosimus may Agnarsson, 2005
'now' Apopyllus now Platnick & Shadab, 1984
'nur' Diospyros nur Ritter, N. & De la Barra, N. 2016
'once' Heterospilus once Marsh, 2013
'our' Mugil our Forsskål, 1775
'pas' Cantabroplectus pas Struyve, 2018
'plus' Rubus plus L.H.Bailey
'qui' Willowsia qui Zhang, Chen & Deharveng, 2011
'that' Xerolinus that (Steiner, 2006)
'this' Xerolinus this (Steiner, 2006)
'une' Trechiama une Ueno, 2001