#Usage
##Development Environment
Run Groovy script: _scripts/RunRiskAnalytics.groovy_
with VM options:
```
-DrunAsUserName=actuary -Dgrails.env=development -DgridGainIpAddress=127.0.0.1 -DnodeMappingStrategy=org.pillarone.riskanalytics.core.simulation.engine.grid.mapping.OneNodeStrategy
```
respectively on Ignite branch use VM options:
```
-DrunAsUserName=actuary -Dgrails.env=development -DgridGainIpAddress=127.0.0.1 -DnodeMappingStrategy=org.pillarone.riskanalytics.core.simulation.engine.grid.mapping.LocalNodesExcludingHeadStrategy
```