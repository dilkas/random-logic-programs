import os
import random
import subprocess
import sys
import yaml

NUM_FACTS = 100000
AVERAGE_SPOTS_PER_CONSTANT = 2 # This determines the domain size after we generate a template
PROPORTION_PROBABILISTIC = 0.5

# Not expected to change
PROBABILITIES = [x/10 for x in range(1, 10)]
PROGRAMS_DIR = '../generated/programs/'
OUTPUT_DIR = '../generated/problog_output/'
CONFIG_FILENAME = '../config.yaml'

def next_name(name):
    'Generate an infinite sequence of strings'
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
    'Generate n constants'
    s = ['a']
    for _ in range(n - 1):
        s.append(next_name(s[-1]))
    return ['c' + x for x in s]

config = yaml.load(open(CONFIG_FILENAME, 'r'), Loader=yaml.Loader)
predicates_and_arities = list(zip(config['predicates'], config['arities']))

def generate_fact(domain):
    'Generate a single fact'
    predicate, arity = random.choice(predicates_and_arities)
    constants = random.choices(domain, k=arity)
    return predicate + '(' + ','.join(constants) + ')'

def facts_to_string(facts, domain):
    'Turn a fact sheet into a string'
    num_probabilistic = int(round(PROPORTION_PROBABILISTIC * NUM_FACTS, 0))
    factsheet = []
    for i, fact in enumerate(facts):
        if i < num_probabilistic:
            factsheet.append(str(random.choice(PROBABILITIES)) + ' :: ' + fact + '.')
        else:
            factsheet.append(fact + '.')

    query = generate_fact(domain)
    return '\n'.join(factsheet) + '\n' + 'query({}).\n'.format(query)

def generate_template():
    return random.choice(predicates_and_arities)

def count_spots(templates):
    return sum([arity for _, arity in templates])

def generate_factsheet():
    factsheet = set()
    domain = None
    # While we don't have a fact sheet of the right size
    while (len(factsheet) < NUM_FACTS):
        # Generate remaining templates
        templates = [generate_template() for _ in range(NUM_FACTS - len(factsheet))]
        # If a domain doesn't exist
        if domain is None:
            # Create a domain based on the number of spots
            domain = generate_constants(count_spots(templates))
        # Fill in the spots with constants at random and put the new facts into the set of facts
        for predicate, arity in templates:
            factsheet.add(predicate + '(' + ','.join(random.choices(domain, k=arity)) + ')')
    return factsheet, domain

def append_factsheets():
    'Append fact sheet to existing programs'
    for filename in os.listdir(PROGRAMS_DIR):
        if filename.endswith('.pl'):
            with open(PROGRAMS_DIR + filename, 'a+') as f:
                factsheet, domain = generate_factsheet()
                f.write(facts_to_string(factsheet, domain))

def remove_programs(d=PROGRAMS_DIR):
    'Remove files from a directory'
    for filename in os.listdir(d):
        if filename.endswith('.pl'):
            os.remove(d + filename)

def run_problog():
    'Run a set of experiments'
    for filename in os.listdir(PROGRAMS_DIR):
        sys.stdout.write('.')
        sys.stdout.flush()
        if filename.endswith('.pl'):
            output = subprocess.Popen(['problog', '-v', PROGRAMS_DIR + filename, '-t', '60'],
                                      stdout=subprocess.PIPE)
            output_str = output.communicate()[0][:-1].decode('utf-8')
            with open(OUTPUT_DIR + filename, 'w+') as f:
                f.write(output_str)

remove_programs()
remove_programs(OUTPUT_DIR)
generator = subprocess.Popen(['mvn',  'exec:java', '-Dexec.mainClass=model.GeneratePrograms', '-Dexec.classpathScope=runtime'], cwd="../")
generator.wait()
append_factsheets()
run_problog()
