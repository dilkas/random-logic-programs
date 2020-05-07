import argparse
import os
import random
import subprocess
import sys
import yaml

# Not expected to change
PROBABILITIES = [x/10 for x in range(1, 10)]
PROGRAMS_DIR = '../generated/programs/'
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

def generate_constants(n, prefix='c'):
    'Generate n constants'
    if (n == 0):
        return []
    s = ['a']
    for _ in range(n - 1):
        s.append(next_name(s[-1]))
    return [prefix + x for x in s]

def generate_fact(domain):
    'Generate a single fact'
    predicate, arity = random.choice(predicates_and_arities)
    constants = random.choices(domain, k=arity)
    return predicate + '(' + ','.join(constants) + ')'

def facts_to_string(facts, domain, args):
    'Turn a fact sheet into a string'
    num_probabilistic = int(round(args.probabilistic * args.num_facts, 0))
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

def generate_factsheet(args):
    factsheet = set()
    domain = None
    # While we don't have a fact sheet of the right size
    while (len(factsheet) < args.num_facts):
        # Generate remaining templates
        templates = [generate_template() for _ in range(args.num_facts - len(factsheet))]
        # If a domain doesn't exist
        if domain is None:
            # Create a domain based on the number of spots
            domain = generate_constants(int(count_spots(templates) / args.spots_per_constant))
        # Fill in the spots with constants at random and put the new facts into the set of facts
        for predicate, arity in templates:
            factsheet.add(predicate + '(' + ','.join(random.choices(domain, k=arity)) + ')')
    return factsheet, domain

def append_factsheets(args):
    'Append fact sheet to existing programs'
    for filename in os.listdir(PROGRAMS_DIR):
        if filename.endswith('.pl'):
            with open(PROGRAMS_DIR + filename, 'a+') as f:
                factsheet, domain = generate_factsheet(args)
                f.write(facts_to_string(factsheet, domain, args))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('num_facts', type=int, help='the number of facts to generate')
    parser.add_argument('spots_per_constant', type=int, help='the average number of times each constant is repeated')
    parser.add_argument('probabilistic', type=float, help='the proportion of facts that should be probabilistic')
    args = parser.parse_args()
    config = yaml.load(open(CONFIG_FILENAME, 'r'), Loader=yaml.Loader)
    predicates_and_arities = list(zip(config['predicates'], config['arities']))
    append_factsheets(args)
