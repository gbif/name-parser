# latin-endings.txt
The suffices were generated querying the genus names in checklistbank. All endings with 4 chars that existed in at least 100 different genus names were added.
The query used was:

   select substring(genus from '....$') as suffix from (select distinct genus_or_above as genus from name) as genera group by suffix having count(*) > 100;

To sort the suffices by their ending linux provides a nice tool:

   $ cat latin-endings.txt | rev | sort | rev > latin-endings.txt2