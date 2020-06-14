from functii_comune import *
from nltk.tokenize import word_tokenize
from pymongo import MongoClient

client = MongoClient("localhost", 27017)
db = client.stabilaRIW


#pt fiece fisier vom avea lista de cuvinte
def indexareDirecta():
    files_list = getListOfFiles("Files")
    file1 = open("Indexare_directa.txt","w") 

    for file in files_list:
        file_d = open(file, "r")
        ps = PorterStemmer()

        #regex = r'[a-zA-Z0-9]+' 
        data = file_d.read().replace('\n', ' ')

        stop_words = open("stop-words.txt",'r').readlines()
        exceptions = open("exceptions.txt",'r').readlines()

        wordslist = []
        stopWords_list = []
        exceptions_list = []
        start_list = []  #lista initiala de cuvinte din fisier

		#modificam fisierele pt a nu mai avea \n dupa fiece linie(acea linie contine un sg token si un \n)
        for word in stop_words: 
            stopWords_list.append(word.replace("\n", ""))

		#la fel si in acest caz
        for word in exceptions:
            exceptions_list.append(word.replace("\n", ""))
		
		#la inceput am inlcuit \n cu "", iar acum facem split la tot textul pt a obtine lista cu toate cuv din fisier
        for word in data.split():
            start_list.append(word)

		#facem diferenta intre cele 2 liste si vom obt numai cuv care sunt stopword-uri 100%
        difference_list = difference(stopWords_list, exceptions_list)
		
        for word in data.split(): #data contine lista de cuv. din fis curent
            #plec de la premisa ca e cuvant de dictionar
            dict_word = 1
            for stop_word in difference_list:
                if (word == stop_word):
                    dict_word = 0 
            if dict_word == 1:
                wordslist.append(word) #adaugam in lista cuv de dictionar

        wordcounter = Counter(wordslist) 
        dict0 = {}

        #scrirea in fisier
        file1.write("\n")
        file1.write(file)
        file1.write("{ \n")
        
        for wd in wordcounter:
            file1.write("\t")
            
            rootWord = ps.stem(wd)
            file1.write(rootWord)
            file1.write(":")
            dict0[rootWord] = wordcounter.get(wd)
            file1.write(str(dict0[rootWord]))
            file1.write("\n")
        file1.write("}")

        sendToBD = {
            "name": file,
            "value": dict0,
        }

       # db.indexare_directa.insert_one(sendToBD)     
    file1.close() 
print("Sf Indexarea Directa.\n")
