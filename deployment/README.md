<!-- Deployment instructions added below -->
## Deployment
Install/Upgrade:
```
helm upgrade --install petclinic charts/petclinic  -n petclinic
```

## Agentic (dev)
Is being used the **dev team** to develope and test the agentic feature.<br/>
```sh
helm upgrade petclinic . -n pet-agentic-dev -f values-agentic-dev.yaml --install --create-namespace
```

## Agentic (Demo)
Is being used by **Roni** for demos.<br/>
Install:
```sh
helm upgrade petclinic . -n pet-agentic-demo -f values-agentic-demo.yaml --install --create-namespace
```