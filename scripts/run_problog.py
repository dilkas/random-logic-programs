import multiprocessing
import os
import subprocess
import sys

PROGRAMS_DIR = '../generated/full_programs/'
OUTPUT_DIR = '../generated/problog_output/'
TIMEOUT = '60'

def run_one(filename):
    sys.stdout.write('.')
    sys.stdout.flush()
    output = subprocess.Popen(['problog', '-v', PROGRAMS_DIR + filename, '-t', TIMEOUT],
                              stdout=subprocess.PIPE)
    output_str = output.communicate()[0][:-1].decode('utf-8')
    with open(OUTPUT_DIR + filename, 'w+') as f:
        f.write(output_str)


files = [f for f in os.listdir(PROGRAMS_DIR) if f.endswith('.pl')]
with multiprocessing.Pool(7) as p:
    p.map(run_one, files)
