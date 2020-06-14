from functii_comune import *
from pymongo import MongoClient

client = MongoClient("localhost", 27017)
db = client.stabilaRIW

def indexareIndirecta():
    #fisierul de output
    file1 = open("Indexare_indirecta.txt","w") 

    db.indexare_indirecta.drop()
    words_list = []  #lista de cuvinte

    #ma folosesc de indexarea directa
    cursor = db.indexare_directa.find()

    for value in cursor:
        for word in value['value'].keys():
            words_list.append(word)
    words_list = list(set(words_list)) #ma asigur ca nu am termeni care se repeta in lista
    
    for word in words_list:
        list_docsName = []
        cursor = db.indexare_directa.find()

        file1.write("\n"+ word +"{")
    
        for line in cursor:
            text = line['value']
            if word in text:
                doc_name = line['name']
                counter= line['value'][word]
                file1.write(" <"+doc_name +">"+ ":"+ str(counter)+" ")
        file1.write("}")
            
        list_docsName.append("<" + doc_name + ">" + ":" + str(counter))

        sendToBD = {
            "name": word,
            "value": list_docsName
        }
		
        db.indexare_indirecta.insert_one(sendToBD)
    file1.close()
print("Sf indexareIndirecta().\n")
