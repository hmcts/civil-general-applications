#!/usr/bin/env bash

set -ex

definition_input_dir=$(realpath 'ccd-definition')
definition_output_file="$(realpath ".")/build/ccd-development-config/ccd-civil-dev.xlsx"
params="$@"

params="$1"
branchName="$2"

#Checkout specific branch pf  civil camunda bpmn definition
git clone https://github.com/hmcts/civil-ccd-definition.git
cd civil-ccd-definition

echo "Switch to ${branchName} branch on civil-ccd-definition"
git checkout ${branchName}


definition_input_dir=$(realpath './ccd-definition')
definition_output_file="$(realpath ".")/ccd-definition/build/ccd-development-config/ccd-civil-dev.xlsx"


./bin/utils/import-ccd-definition.sh "${definition_input_dir}" "${definition_output_file}" "${params}"
rm -rf ./civil-ccd-definition
cd ..
