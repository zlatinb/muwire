#!/usr/bin/python3
  
import os,sys,json

if len(sys.argv) < 2 :
    print("This script counts unique hosts in the MuWire network",file = sys.stderr)
    print("Pass the prefix of the files to analyse.  For example:",file = sys.stderr)
    print("\"20200427\" will count unique hosts on 27th of April 2020",file = sys.stderr)
    print("\"202004\" will count unique hosts during all of April 2020",file = sys.stderr)
    sys.exit(1)

day = sys.argv[1]
files = os.listdir(".")
files = [x for x in files if x.startswith(day)]

hosts = set()

for f in files:
    for line in open(f):
        host = json.loads(line)
        hosts.add(host["destination"])

print(len(hosts))
