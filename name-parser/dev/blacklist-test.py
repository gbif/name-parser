
import urllib.request, json


SPECIES_WS = "http://api.gbif.org/v1/species/search?qField=SCIENTIFIC&q="
EPI_PROPS = ["genus", "genus", "genus", "genus", "genus"]
with open("blacklist-epithets.txt") as f:
    for stop in f:
        stop=stop.strip();
        print(stop, end="\t")
        with urllib.request.urlopen(SPECIES_WS+stop) as url:
            data = json.loads(url.read().decode())
            #print(data)
            if data["results"]:
                print(str(data["count"]) + "  " + data["results"][0]["scientificName"])
                found=None
                for rec in data["results"]:
                    if "canonicalName" in rec.keys():
                        name = rec["canonicalName"];
                        if stop in name:
                            found=rec
                            break
                if found:
                    print("  \t!!! IN NAME: " + found["scientificName"])
            else:
                print("--")
