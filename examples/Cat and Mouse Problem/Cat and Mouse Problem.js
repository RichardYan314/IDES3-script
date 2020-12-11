// this example is the Cat and Mouse Problem, taken from:
// Wonham, W. M. & Ramadge, P. J.
// On the Supremal Controllable Sublanguage of a Given Language 
// SIAM Journal on Control and Optimization, Society for Industrial & Applied Mathematics (SIAM), 1987, 25, 637-659
// Section 7.3. Example 5.


// removes all opened models
// without prompting of unsaved models
IDES.removeAllNoPrompt();


// load plants
Gc = IDES.importYAML('Plant - Cat.yml')
Gm = IDES.importYAML('Plant - Mouse.yml')


// compute joint behaviour
G = sync(Gc, Gm)
G.setName("Joint Behaviour")


// compute legal specification programmatically
// by removing all states in the form of (i, i)
// and then trim
E = G.clone()
stateIter = E.getStateIterator()
while (stateIter.hasNext()) {
  state = stateIter.next()
  stateName = state.getName().slice(1,-1)
  let [catState, mouseState] = stateName.split(',')
  if (catState == mouseState) {
    stateIter.remove() // avoid concurrent modification exception
    E.remove(state)
  }
}
E = trim(E)
E.setName("Specification")


// compute supcon
SG = supcon(G, E)
SG.setName("SG")


// technically we are done here.
// but supcon produces states in the form `((i, j), (i, j))`
// i.e., with duplicate information
// so it is preferrable to remove them

// it is rather hard to modify a model
// once it is in the workspace,
// since modifying the model won't update
// its graph layout.
// hence we need to remove the model from the workspace,
// do the modification,
// and finally add it back to triger a redraw

// remove the model from workspace
// use `modelSaved` to trick the model
// to avoid prompt for saving
SG.modelSaved()
Hub.getWorkspace().removeModel(SG.getName())


// change state labels from `((i, j), (i, j))`
// to just `(i, j)`
stateIter = SG.getStateIterator()
while (stateIter.hasNext()) {
  state = stateIter.next()
  stateName = state.getName().slice(1,-1)
  stateName = stateName.split(',').slice(0,2).join()
  // have to update both the state name
  // as well as the text of its graph layout element
  state.setName(stateName)
  state.getAnnotation("layout").setText(stateName)
}

// add model back to workspace
IDES.addModel(SG)