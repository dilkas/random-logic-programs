import csv
import os

DIR = '../generated/problog_output/'

data = []
for filename in os.listdir(DIR):
    d = {'instance': filename[:filename.index('.')]}
    if not filename.endswith('.pl'):
        continue
    with open(DIR + filename, 'r') as f:
        for line in f:
            if line.startswith('[INFO]'):
                if line.startswith('[INFO] Output level'):
                    continue
                name = line[(line.index(' ')+1):line.index(':')]
                time = line[(line.rindex(' ')+1):line.rindex('s')]
                d[name] = time
            elif line.startswith('NonGroundProbabilisticClause'):
                d['answer'] = None
            else:
                d['answer'] = ''.join([c for c in line if c.isdigit() or c == '.'])
    data.append(d)

fieldnames = set()
for d in data:
    fieldnames.update(d.keys())

with open('../data/output.csv', 'w', newline='') as csvfile:
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
    writer.writeheader()
    for d in data:
        writer.writerow(d)
