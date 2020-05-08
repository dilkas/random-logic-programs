import itertools
import os
import random
import subprocess
import sys
import yaml

from generate_strings import generate_constants

# Not expected to change
PROBABILITIES = [x/10 for x in range(1, 10)]
INPUT_DIR = '../generated/programs/'
OUTPUT_DIR = '../generated/full_programs/'
CONFIG_FILENAME = '../config.yaml'

def facts_to_string(facts, proportion_probabilistic, query):
    'Turn a fact sheet into a string'
    num_probabilistic = int(round(proportion_probabilistic * len(facts), 0))
    factsheet = []
    for i, fact in enumerate(facts):
        if i < num_probabilistic:
            factsheet.append(str(random.choice(PROBABILITIES)) + ' :: ' + fact + '.')
        else:
            factsheet.append(fact + '.')

    return '\n'.join(factsheet) + '\n' + 'query({}).\n'.format(query)

def generate_universe(predicates_and_arities, domain):
    return [predicate + '(' + ','.join(arguments) + ')'
            for predicate, arity in predicates_and_arities
            for arguments in itertools.product(domain, repeat=arity)]

def generate_factsheet(predicates_and_arities, proportion_listed, domain):
    universe = generate_universe(predicates_and_arities, domain)
    s = random.sample(universe, int(len(universe) * proportion_listed))
    return s[:-1], s[-1]

def generate_filename(config, count):
    return '_'.join(str(x) for x in [len(config.predicates),
                                     len(config.variables),
                                     config.maxNumNodes,
                                     max(config.arities),
                                     len(config.independentPairs), count]
    ) + '.pl'

def generate_new_filename(config, count, domain_size, proportion_listed, proportion_probabilistic):
    old_filename = generate_filename(config, count)
    old_filename = old_filename[:old_filename.index('.')]
    return old_filename + '_' + '_'.join(str(x) for x in
                                         [domain_size, proportion_listed, proportion_probabilistic]) + '.pl'

def append_factsheets(config, count):
    predicates_and_arities = list(zip(config.predicates, config.arities))
    filename = INPUT_DIR + generate_filename(config, count)
    with open(filename, 'r') as f:
        rules = f.read()
    for proportion_listed, proportion_probabilistic in itertools.product([0.25, 0.5, 0.75], repeat=2):
        for domain_size in [1000, 10000, 100000]:
            domain = generate_constants(domain_size)
            factsheet, query = generate_factsheet(predicates_and_arities, proportion_listed, domain)
            facts = facts_to_string(factsheet, proportion_probabilistic, query)
            new_filename = OUTPUT_DIR + generate_new_filename(
                config, count, domain_size, proportion_listed, proportion_probabilistic)
            with open(new_filename, 'w') as f:
                f.write(rules + facts)
