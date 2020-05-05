import os
import random
import subprocess
import sys
import yaml

NUM_FACTS = 100000
AVERAGE_SPOTS_PER_CONSTANT = 2
PROPORTION_PROBABILISTIC = 0.5

# Not expected to change
PROBABILITIES = [x/10 for x in range(1, 10)]
PROGRAMS_DIR = './programs/'
OUTPUT_DIR = './output/'

def next_name(name):
    if (name[-1] != 'z'):
        return name[:-1] + chr(ord(name[-1]) + 1)

    i = len(name) - 1
    while (i >= 0 and name[i] == 'z'):
        i -= 1
    first_z = i + 1

    count_z = len(name) - first_z
    if first_z >= 1:
        return name[:(first_z-1)] + chr(ord(name[first_z-1]) + 1) + 'a' * count_z
    return 'a' * (count_z + 1)

def generate_constants(n):
    s = ['a']
    for _ in range(n - 1):
        s.append(next_name(s[-1]))
    return ['c' + x for x in s]

#domain = generate_constants(DOMAIN_SIZE)
config = yaml.load(open('config.yaml', 'r'), Loader=yaml.Loader)

def generate_fact(domain):
    predicates_and_arities = list(zip(config['predicates'], config['arities']))
    predicate, arity = random.choice(predicates_and_arities)
    constants = random.choices(domain, k=arity)
    return predicate + '(' + ','.join(constants) + ')', constants

# TODO: rework so that predicate and arities are generated before constants
def generate_factsheet():
    facts = set()
    all_constants = []
    while len(facts) < NUM_FACTS:
        fact, constants = generate_fact()
        all_constants += constants
        facts.add(fact)

    num_probabilistic = int(round(PROPORTION_PROBABILISTIC * NUM_FACTS, 0))
    factsheet = []
    for i, fact in enumerate(facts):
        if i < num_probabilistic:
            factsheet.append(str(random.choice(PROBABILITIES)) + ' :: ' + fact + '.')
        else:
            factsheet.append(fact + '.')

    query, _ = generate_fact(all_constants)
    return '\n'.join(factsheet) + '\n' + 'query({}).\n'.format(query)

def append_factsheets():
    for filename in os.listdir(PROGRAMS_DIR):
        if filename.endswith('.pl'):
            with open(PROGRAMS_DIR + filename, 'a+') as f:
                f.write(generate_factsheet())

def remove_programs(d=PROGRAMS_DIR):
    for filename in os.listdir(d):
        if filename.endswith('.pl'):
            os.remove(d + filename)

def run_problog():
    for filename in os.listdir(PROGRAMS_DIR):
        sys.stdout.write('.')
        sys.stdout.flush()
        if filename.endswith('.pl'):
            output = subprocess.Popen(['problog', '-v', PROGRAMS_DIR + filename],
                                      stdout=subprocess.PIPE)
            output_str = output.communicate()[0][:-1].decode('utf-8')
            with open(OUTPUT_DIR + filename, 'w+') as f:
                f.write(output_str)

remove_programs()
remove_programs(OUTPUT_DIR)
subprocess.call(['mvn',  'exec:java', '-Dexec.mainClass=model.GeneratePrograms', '-Dexec.classpathScope=runtime'])
append_factsheets()
run_problog()
