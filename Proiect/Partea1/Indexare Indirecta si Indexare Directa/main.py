from indexare_indirecta import *
from indexare_directa import *


import time

t0 = time.time()
print("\n\n")
indexareDirecta()
t1 = time.time()
timeF = t1 - t0
print("\n Time indexare directa:", timeF)

t0 = time.time()
print("\n\n")
indexareIndirecta()
t1 = time.time()
timeF = t1 - t0
print("\n Time indexare indirecta:", timeF)

