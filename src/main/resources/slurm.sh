#!/usr/bin/env bash

problems=$(seq 1 9)
learners=(ADT DHC DiscriminationTree KearnsVazirani ExtensibleLStar MalerPnueli RivestSchapire TTT)
auts=("-M" "-B" "-M -B")
bbos=("" "-C" "-D")

LOG=$(date +%s)"-$1"
mkdir -p "$LOG"

export CLASSPATH="$CLASSPATH:../../../target/RERS-1.0-SNAPSHOT-jar-with-dependencies.jar"

for problem in $problems; do
	for learner in "${learners[@]}"; do
		for aut in "${auts[@]}"; do
			for bbo in "${bbos[@]}"; do
				name="problem-$problem-$learner-$aut-$bbo"
				name=${name// /_}
				file="$LOG/$name"
				multiplier=$((5 * problem))
				srun "-J$name" -N1 -c7 -t300 --output="$file.csv" --error="$file.log" java "nl.utwente.fmt.rers.Main" "-c" "-t3600" "-m$multiplier.0" "-u20" "$problem" "$learner" $aut $bbo &
			done;
		done
	done
done

wait