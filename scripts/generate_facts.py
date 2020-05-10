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
MAX_PROPORTION_LISTED = 0.75

def facts_to_string(facts, proportion_probabilistic, query):
    'Turn a fact sheet into a string'
    num_probabilistic = int(round(proportion_probabilistic * len(facts), 0))
    factsheet = []
    for i, fact in enumerate(facts):
        factsheet.append(str(random.choice(PROBABILITIES)) + ' :: ' + fact + '.'
                         if i < num_probabilistic else fact + '.')
    return '\n'.join(factsheet) + '\n' + 'query({}).\n'.format(query)

def generate_factsheet(predicates_and_arities, num_facts, domain):
    facts = set()
    total = 0
    correct = 0
    while len(facts) < num_facts + 1:
        predicate, arity = random.choice(predicates_and_arities)
        constants = random.choices(domain, k=arity)
        previous_len = len(facts)
        facts.add(predicate + '(' + ','.join(constants) + ')')
        if len(facts) > previous_len:
            correct += 1
        total += 1
    query = random.sample(facts, 1)[0]
    print('{:.2f}%'.format(100*correct/total))
    return facts.difference(set(query)), query

def generate_filename(config, count):
    return '_'.join(str(x) for x in [len(config.predicates),
                                     len(config.variables),
                                     config.maxNumNodes,
                                     max(config.arities),
                                     len(config.independentPairs), count]
    ) + '.pl'

def generate_new_filename(config, count, domain_size, num_facts, proportion_probabilistic, universe_size):
    old_filename = generate_filename(config, count)
    old_filename = old_filename[:old_filename.index('.')]
    return old_filename + '_' + '_'.join(str(x) for x in
                                         [domain_size, num_facts, proportion_probabilistic, universe_size]) + '.pl'

def append_factsheets(config, count):
    predicates_and_arities = list(zip(config.predicates, config.arities))
    filename = INPUT_DIR + generate_filename(config, count)
    with open(filename, 'r') as f:
        rules = f.read()
    for domain_size in [100, 200, 400]:
        for num_facts in [1000, 10000, 100000]:
            universe_size = sum(domain_size**arity for _,arity in predicates_and_arities)
            num_facts = min(num_facts, MAX_PROPORTION_LISTED * universe_size)
            print(num_facts, 'out of', universe_size)
            for proportion_probabilistic in [0.25, 0.5, 0.75]:
                domain = generate_constants(domain_size)
                factsheet, query = generate_factsheet(predicates_and_arities, num_facts, domain)
                facts = facts_to_string(factsheet, proportion_probabilistic, query)
                new_filename = OUTPUT_DIR + generate_new_filename(
                    config, count, domain_size, num_facts, proportion_probabilistic, universe_size)
                with open(new_filename, 'w') as f:
                    f.write(rules + facts)
