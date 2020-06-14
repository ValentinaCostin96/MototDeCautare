import os
import re  #  "Regular Expresion"
from collections import Counter
from nltk.stem import PorterStemmer

#fct returneaza (dupa ce o creaza) listele de cuvinte pentru fiecare fisier
def get_words():
    list_of_words = []
    files = getListOfFiles("./Files")
    print("files here:\n")
    print(files)
    for file in files:
        with open(file, "r") as fis:
            for line in fis:
                text = re.findall(r"'(.*?)'", line)
                for word in text:
                    #daca nu am mai intalnit cuvantul, at il adaug in lista de cuvinte
                    if word not in list_of_words:
                        list_of_words.append(word)
    return list_of_words

#fct extrage dintr-un folder doar fisiere .txt cu path-urile lor absolute, intra inclusiv in subfoldere
def getListOfFiles(dirName):
    #luam lista de fisiere din director
    listOfFile = os.listdir(dirName)  

    #declaram o lista pt numele fisierelor si calea lor 
    allFiles = list()

    #parcurgem lista de fisiere 
    for file in listOfFile:
        #luam path-ul absolut pentru fiecare fisier din director
        fullPath = os.path.join(dirName, file)
        
        if os.path.isdir(fullPath):
            allFiles = allFiles + getListOfFiles(fullPath)
        else:
            allFiles.append(fullPath)

    return allFiles

#diferenta intre mult de stop-words si exceptii. Va rezulta doar multimea cu elemente care sunt 100% stop-words
def difference(stop_words, exceptions):
    return set(stop_words).difference(set(exceptions))
