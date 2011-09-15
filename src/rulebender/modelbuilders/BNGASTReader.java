package rulebender.modelbuilders;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rulebender.modelbuilders.ruledata.ComponentData;
import rulebender.modelbuilders.ruledata.RuleData;
import rulebender.modelbuilders.ruledata.RulePatternData;

import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

//import sun.awt.ComponentFactory;

import rulebender.models.contactmap.CMapModel;
import rulebender.models.contactmap.Bond;
import rulebender.models.contactmap.Component;
import rulebender.models.contactmap.Molecule;
import rulebender.models.contactmap.Rule;

import bngparser.grammars.BNGGrammar.prog_return;


/**
 * This class works as the reader/director in a builder pattern.  To use it,
 * you must create an implementation of the ModelBuilderInterface, register it by 
 * creating a new instance of this class, and then call the buildWithAST(prog_return ast)
 * method.
 * 
 * It is important to note that the builder will receive only the information that is in the
 * ast.  All data structures, registries, construction, etc. is the responsibility of the builder.
 * The id's for the parser will be how the molecules, bonds, and rules will be referenced by 
 * the parser, so be sure to take advantage of that.  i.e. if a builder receives bond information
 * it should look at the ids of the sites to find out what the molecules/components/states were
 * for the bond.
 * 
 * @author ams292
 */
public class BNGASTReader 
{
	ModelBuilderInterface m_builder;
	
	public BNGASTReader(ModelBuilderInterface builder)
	{
		setBuilder(builder);
	}
		
	public void buildWithAST(prog_return ast)
	{
		// Get the XML Document.
		Document doc = getDocument(ast);
		
        // The root of the document is the sbml tag.  Get the Model node.
		Element model = doc.getRootElement().getChild("Model", doc.getRootElement().getNamespace());
		
		// handle the parameters
		readParametersBlock(model.getChild("ListOfParameters", model.getNamespace()));
		
		// handle the molecule types
		readMoleculeTypesBlock(model.getChild("ListOfMoleculeTypes", model.getNamespace()));
		
		// handle the species
		readSpeciesBlock(model.getChild("ListOfSpecies", model.getNamespace()));
		
		// handle the compartments
		readCompartmentsBlock(model.getChild("ListOfCompartments", model.getNamespace()));
		
		// handle the reaction rules
		readReactionRuleBlock(model.getChild("ListOfReactionRules", model.getNamespace()));
		
		// handle the observables
		readObservablesBlock(model.getChild("ListOfObservables", model.getNamespace()));
	}

	/**
	 *  handle the parameters
	 * Example:
			<ListOfParameters>
      			<Parameter id="Lig_tot" type="Constant" value="6000.0"/>
      			<Parameter id="Rec_tot" type="Constant" value="400.0"/>
      			<Parameter id="Lyn_tot" type="Constant" value="28.0"/>
      			...
      		</ListOfParameters>
	 * @param paramBlockNode
	 * @throws DataConversionException 
	 */
	public void readParametersBlock(Element paramBlockNode) 
	{
		// Get the list
		List<Element> paramList = paramBlockNode.getChildren();
		
		// For each element of the list. 
		for(Element param : paramList)
		{
			// Tell the builder that the parameter has been found.
			m_builder.parameterFound(param.getAttributeValue("id"), param.getAttributeValue("type"), param.getAttributeValue("value"));
		}
	}
	

	/**
	 * handle the moleculeTypes
	 * 
	 * Example:
	 	<ListOfMoleculeTypes>
		     <MoleculeType id="Test"/>
		     <MoleculeType id="Lyn">
		       <ListOfComponentTypes>
		             <ComponentType id="U"/>
		             <ComponentType id="SH2"/>  
		       </ListOfComponentTypes>   
		     </MoleculeType>
		  
		     <MoleculeType id="Syk">
		       <ListOfComponentTypes>
		             <ComponentType id="tSH2"/>
		               <ComponentType id="l">   
		               <ListOfAllowedStates>
		                <AllowedState id="Y"/>
		               </ListOfAllowedStates>
		               </ComponentType>
		               <ComponentType id="a">   
		               <ListOfAllowedStates>
		                <AllowedState id="Y"/>
		               </ListOfAllowedStates>
		               </ComponentType>  
		       </ListOfComponentTypes>   
		      </MoleculeType>
		      ...
		    </ListOfMoleculeTypes>
		    
	 * @param moleculeTypesBlockNode
	 */
	public void readMoleculeTypesBlock(Element moleculeTypesBlockNode)
	{
		// Get the list
		List<Element> moleculeTypesList = moleculeTypesBlockNode.getChildren();
		
		// For each moleculeType
		for(Element moleculeType : moleculeTypesList)
		{
			
			Molecule molecule = new Molecule(moleculeType.getAttributeValue("id"));
			
			// Check to see if there are components for this molecule.
			if(moleculeType.getChild("ListOfComponentTypes", moleculeType.getNamespace()) == null)
			{
				// Go to the next moleculeType if there are not any components.
				continue;
			}
			
			// Get the node and then list for the components
			Element componentTypeListNode = moleculeType.getChild("ListOfComponentTypes", moleculeType.getNamespace());
			List<Element> componentTypeList = componentTypeListNode.getChildren();
			
			// For each of the components.
			for(Element componentType : componentTypeList)
			{
				molecule.addComponent(new Component(componentType.getAttributeValue("id")));
				
				// Skip to the next componentType if there are no states. 
				if(componentType.getChild("ListOfAllowedStates", componentType.getNamespace()) == null)
				{
					continue;
				}
				
				// Get the list node and then list.
				Element allowedStatesListNode = componentType.getChild("ListOfAllowedStates", componentType.getNamespace());
				List<Element> allowedStatesList = allowedStatesListNode.getChildren();
				
				for(Element allowedState : allowedStatesList)
				{
					molecule.addStateToComponent(allowedState.getAttributeValue("id"), componentType.getAttributeValue("id"));
				} // done with states
			} // done with components types
			
			m_builder.foundMoleculeType(molecule);
			
		} // done with molecules types
	}
	
	/**
	 * handle the species block
	 * 
	 * Example:
	    <ListOfSpecies>
	    	<Species id="S1"  concentration="Lig_tot" name="Lig(l,l)">
				<ListofMolecules>
		       		<Molecule id="S1_M1" name="Lig" >
		            	<ListOfComponents>
		                	<Component id="S1_M1_C1"  name="l"  numberOfBonds="0" />
		                	<Component id="S1_M1_C2"  name="l"  numberOfBonds="0" />
		              	</ListOfComponents>
		            </Molecule>
		        </ListofMolecules>
	      	</Species>
	      	...
	     	<Species id="S4"  concentration="Rec_tot" name="Rec(a!1,b~Y!2,g~Y).Lig(l!1,l!2)">
		    	<ListofMolecules>
		        	<Molecule id="S4_M1" name="Rec" >
		            	<ListOfComponents>
		                	<Component id="S4_M1_C1"  name="a"  numberOfBonds="1" />
			                <Component id="S4_M1_C2"  name="b"  state= "Y"  numberOfBonds="1" />
			                <Component id="S4_M1_C3"  name="g"  state= "Y"  numberOfBonds="0" />
		              	</ListOfComponents>
		            </Molecule>
		            <Molecule id="S4_M2" name="Lig" >
		            	<ListOfComponents>
			            	<Component id="S4_M2_C1"  name="l"  numberOfBonds="1" />
			                <Component id="S4_M2_C2"  name="l"  numberOfBonds="1" />
		              	</ListOfComponents>
		            </Molecule>
				</ListofMolecules>
		        <ListofBonds>
		            <Bond id="S4_B1" site1="S4_M1_C1"  site2="S4_M2_C1" />
		            <Bond id="S4_B2" site1="S4_M1_C2"  site2="S4_M2_C2" />
		        </ListofBonds>
			</Species>
	  	</ListOfSpecies.
	 * 
	 * 
	 * @param speciesNode
	 */
	private void readSpeciesBlock(Element speciesNode) 
	{
		// Get the list of Species Element nodes.
		List<Element> speciesList = speciesNode.getChildren();
		
		// For each Species Node
		for(Element species : speciesList)
		{
			//TODO  Do something about creating a species datatype or at least reporting 
			// it if necessary
			
			// This keeps track of the molecule objects so we can get the name of
			// the molecule from the bond information
			HashMap<String, String> moleculeNameForID = new HashMap<String, String>();
			
			// We also need to keep track of the components name for the bond information
			// because the bond takes place at the component level.  We can 
			// get the state of the component for the bond from the component object.
			HashMap<String, Component> componentNameForID = new HashMap<String, Component>(); 
			
			// A species can contain multiple molecules.  Get the molecules list for this species. 
			Element moleculesNode = species.getChild("ListofMolecules", species.getNamespace());
			List<Element> moleculesList = moleculesNode.getChildren();
			
			// For each of the molecules
			for(Element moleculeEntry : moleculesList)
			{
				// Get the molecule name.
				String moleculeName = moleculeEntry.getAttributeValue("name");
				
				// Declare the compartment string.
				String compartment = null;
				
				// Extract the compartment
				if(moleculeName.contains("@"))
				{
					compartment = moleculeName.substring(moleculeName.indexOf("@"));
					moleculeName.substring(0, moleculeName.indexOf("@"));
				}
				
				// Enter the name into the registry
				moleculeNameForID.put(moleculeEntry.getAttributeValue("id"), moleculeName);
				
				// Create the object
				Molecule molecule = new Molecule(moleculeName);
				
				// Add the compartment
				if(compartment != null)
				{
					molecule.addCompartment(compartment);
				}
				
				// A molecule can have many components. Get the component list for this molecule.
				Element componentListNode = moleculeEntry.getChild("ListOfComponents", moleculeEntry.getNamespace());
				List<Element> componentList = componentListNode.getChildren();
				
				// For each of the components.
				for(Element componentElement : componentList)
				{	
					// Create the component 
					Component component = new Component(componentElement.getAttributeValue("name"));
					
					// Add it to the molecule
					molecule.addComponent(component);
					
					// Add it to the registry
					componentNameForID.put(componentElement.getAttributeValue("id"), component);
					
					
					//A component can a state
					if(componentElement.getAttribute("state") != null)
					{
						molecule.addStateToComponent(componentElement.getAttributeValue("state"), componentElement.getAttributeValue("name"));
					} // done state
				}// done components
				
				// Tell the builder about this molecule
				m_builder.foundMoleculeInSeedSpecies(molecule);
				
			}// done molecule
						
			// There may be bond information here. 
			Element bondListNode = species.getChild("ListofBonds", species.getNamespace());
			
			if(bondListNode != null)
			{
				List<Element> bonds = bondListNode.getChildren();
				
				for(Element bond : bonds)
				{
					// The ids of the components.
					String comp1 = bond.getAttributeValue("site1");
					String comp2 = bond.getAttributeValue("site2");
					
					// The ids are S#_M#_C#
					
					// the mole id is up to the second underscore
					String mole1id = comp1.substring(0, comp1.lastIndexOf("_"));
					String mole2id = comp2.substring(0, comp2.lastIndexOf("_"));
					
					// Get the names from the registries 
					String moleName1 = moleculeNameForID.get(mole1id);
					String compName1 = componentNameForID.get(comp1).getName();
					int compID1 = componentNameForID.get(comp1).getUniqueID();
					String moleName2 = moleculeNameForID.get(mole2id);
					String compName2 = componentNameForID.get(comp2).getName();
					int compID2 = componentNameForID.get(comp2).getUniqueID();
					
					// See if there are states, and then either assign null or the value.
					String state1 = null;
					
					if(componentNameForID.get(comp1).getStates() != null && componentNameForID.get(comp1).getStates().size() > 0)
					{
						state1 = componentNameForID.get(comp1).getStates().get(0).getName();
					}
					
					String state2 = null;
					
					if(componentNameForID.get(comp1).getStates() != null && componentNameForID.get(comp1).getStates().size() > 0)
					{
						state1 = componentNameForID.get(comp1).getStates().get(0).getName();
					}
					
					// Tell the builder about the bond.
					m_builder.foundBondInSeedSpecies(moleName1, compName1, compID1, state1, moleName2, compName2, compID2, state2);
					
					//TODO In the future, the parser will recognize bonds of the type Ex1().Ex2().  
					
				} // done bonds
			}// done bond list check
		}// done species
	}


	/**
	 * Handles the compartments.
	 *   
	 * @param compartmentsBlockNode
	 */
	public void readCompartmentsBlock(Element compartmentsBlockNode)
	{
		// Get the list.
		List<Element> compartmentList = compartmentsBlockNode.getChildren();
		
		// For each compartment entry
		for(Element compartment : compartmentList)
		{
			m_builder.foundCompartment(compartment.getAttributeValue("id"),compartment.getAttributeValue("size"), compartment.getAttributeValue("outside"));
		}
	}
	
	/**
	 *  Handle the reaction rules
	 *  
	 *  Example:
	  
	  <ListOfReactionRules>
      <ReactionRule id="RR1" name="Rule1">
       <ListOfReactantPatterns>
          <ReactantPattern id="RR1_RP1">  
                   <ListofMolecules>
                     <Molecule id="RR1_RP1_M1" name="Rec" >
                      <ListOfComponents>
                        <Component id="RR1_RP1_M1_C1"  name="a"  numberOfBonds="0" />
                      </ListOfComponents>
                     </Molecule>
                   </ListofMolecules>
           
                </ReactantPattern>
          <ReactantPattern id="RR1_RP2">  
                   <ListofMolecules>
                     <Molecule id="RR1_RP2_M1" name="Lig" >
                      <ListOfComponents>
                        <Component id="RR1_RP2_M1_C1"  name="l"  numberOfBonds="0" />
                        <Component id="RR1_RP2_M1_C2"  name="l"  numberOfBonds="0" />
                      </ListOfComponents>
                     </Molecule>
                   </ListofMolecules>
           
                </ReactantPattern>
       </ListOfReactantPatterns>
       <ListOfProductPatterns>
        <ProductPattern id="RR1_PP1"> 
               <ListofMolecules>
                 <Molecule id="RR1_PP1_M1" name="Rec" >
                  <ListOfComponents>
                    <Component id="RR1_PP1_M1_C1"  name="a"  numberOfBonds="1" />
                  </ListOfComponents>
                 </Molecule>
                 <Molecule id="RR1_PP1_M2" name="Lig" >
                  <ListOfComponents>
                    <Component id="RR1_PP1_M2_C1"  name="l"  numberOfBonds="1" />
                    <Component id="RR1_PP1_M2_C2"  name="l"  numberOfBonds="0" />
                  </ListOfComponents>
                 </Molecule>
               </ListofMolecules>
               <ListofBonds>
                 <Bond id="_B1" site1="RR1_PP1_M1_C1" site2="RR1_PP1_M2_C1" />
               </ListofBonds>
         
            </ProductPattern>
       </ListOfProductPatterns>
      <RateLaw>
        <ListOfRateConstants>
          <RateConstant value="1.328452377888242E-7"/>
        </ListOfRateConstants>
      </RateLaw>
      <Map>
         <MapItem sourceID="RR1_RP1_M1" targetID="RR1_PP1_M1"/>
         <MapItem sourceID="RR1_RP1_M1_C1" targetID="RR1_PP1_M1_C1"/>
         <MapItem sourceID="RR1_RP2_M1" targetID="RR1_PP1_M2"/>
         <MapItem sourceID="RR1_RP2_M1_C1" targetID="RR1_PP1_M2_C1"/>
         <MapItem sourceID="RR1_RP2_M1_C2" targetID="RR1_PP1_M2_C2"/>
         
      </Map>
      <ListOfOperations>
        <AddBond site1="RR1_RP1_M1_C1" site2="RR1_RP2_M1_C1"/>
      </ListOfOperations>
	</ReactionRule>
	...
	</ListOfReactionRules>

	 * @param reactionRulesBlockNode
	 */
	public void readReactionRuleBlock(Element reactionRulesBlockNode)
	{
		// Get the list of reaction rules
		List<Element> reactionRulesList = reactionRulesBlockNode.getChildren();
		
		// For each of the reaction rules.
		for(Element reactionRule : reactionRulesList)
		{
			RuleData ruleData = new RuleData(reactionRule.getAttributeValue("name"));
			
			HashMap<String, String> moleculeNameForID = new HashMap<String, String>();
			HashMap<String, ComponentData> componentDataForID = new HashMap<String, ComponentData>();
			
			//TODO need the expression.
			
			// Get the Reactant Patterns
			Element reactantPatternsNode = reactionRule.getChild("ListOfReactantPatterns", reactionRule.getNamespace());
			List<Element> reactantPatternList = reactantPatternsNode.getChildren();

			// for each of the reactant patterns 
			for(Element reactantPattern : reactantPatternList)
			{
				// Create the intermediate data storage object
				RulePatternData reactantPatternData = new RulePatternData();
				
				// Get the list of molecules in the pattern
				Element reactantPatternMoleculeListNode = reactantPattern.getChild("ListofMolecules", reactantPattern.getNamespace());
				List<Element> reactantPatternMoleculeList = reactantPatternMoleculeListNode.getChildren();
				
				// For each of the reactantPatternMolecules
				for(Element reactantPatternMolecule : reactantPatternMoleculeList)
				{
				
					// Add the molecule to the reactantPatternData
					reactantPatternData.addMolecule(reactantPatternMolecule.getAttributeValue("name"));
					
					// Add the id->name infomration to the molecule registry.
					moleculeNameForID.put(reactantPatternMolecule.getAttributeValue("id"), reactantPatternMolecule.getAttributeValue("name"));
					
					// Molecules have components.  Get the list.
					Element reactantPatternMoleculeComponentListNode = reactantPatternMolecule.getChild("ListOfComponents", reactantPatternMolecule.getNamespace());
					List<Element> reactantPatternMoleculeComponentList = reactantPatternMoleculeComponentListNode.getChildren();
					
					// For each component.
					for(Element reactantPatternMoleculeComponent : reactantPatternMoleculeComponentList)
					{
						// Create the componentdata object 
						ComponentData component = new ComponentData(reactantPatternMoleculeComponent.getAttributeValue("name"));
						
						// Add it to the molecule
						reactantPatternData.addComponentToMolecule(component, reactantPatternMolecule.getAttributeValue("name"));
						
						// Add it to the registry
						System.out.println("Adding ComponentData for id: " + reactantPatternMoleculeComponent.getAttributeValue("id"));
						componentDataForID.put(reactantPatternMoleculeComponent.getAttributeValue("id"), component);
								   
						//There could be a state.
						if(reactantPatternMolecule.getAttributeValue("state") != null)
						{
							// Set the state in the data structure.
							reactantPatternData.setStateForComponentInMolecule(reactantPatternMoleculeComponent.getAttributeValue("state"), 
																			   reactantPatternMoleculeComponent.getAttributeValue("name"),
																			   reactantPatternMolecule.getAttributeValue("name"));
						} // Done with state
					} // done with reactantPatternMoleculeComponent
				} // Done with reactantPatternMolecule
				
				
				// Get the bonds in the reactantPatterns if they exist
				if(reactantPattern.getChild("ListofBonds", reactantPattern.getNamespace()) != null)
				{
					Element reactantPatternBondListNode = reactantPattern.getChild("ListofBonds", reactantPattern.getNamespace());
					List<Element> reactantPatternBondList = reactantPatternBondListNode.getChildren();
					
					// For each of the bonds in the reactantPatterns.
					for(Element reactantPatternBond : reactantPatternBondList)
					{
						// The ids of the components.
						String comp1 = reactantPatternBond.getAttributeValue("site1");
						String comp2 = reactantPatternBond.getAttributeValue("site2");
						

						if(comp2.equals(""))
						{
							// wildcard
							// TODO JUST SKIPPED IT FOR NOW
							continue;
						}
						
						// The ids are S#_M#_C#
						
						// the mole id is up to the second underscore
						System.out.println("Getting last index of _ for: " + comp1);
						String mole1id = comp1.substring(0, comp1.lastIndexOf("_"));
						System.out.println("Getting last index of _ for: " + comp2);
						String mole2id = comp2.substring(0, comp2.lastIndexOf("_"));
						
						// Get the names from the registries 
						String moleName1 = moleculeNameForID.get(mole1id);
						System.out.println("Trying to get component data for ID: " + comp1);
						String compName1 = componentDataForID.get(comp1).getComponent();
						String moleName2 = moleculeNameForID.get(mole2id);
						System.out.println("Trying to get component data for ID: " + comp2);
						System.out.println(comp2 + " is " + (componentDataForID.get(comp2) == null ? "not" : "") + " in the registry");
						String compName2 = componentDataForID.get(comp2).getComponent();
						
						int compID1 = componentDataForID.get(comp1).getUniqueID();
						int compID2 = componentDataForID.get(comp2).getUniqueID();
						
						String state1 = componentDataForID.get(comp1).getState();
						String state2 = componentDataForID.get(comp1).getState();
						
						
						reactantPatternData.addbond(moleName1, compName1, compID1, state1, moleName2, compName2, compID2, state2);
					}	
				}
				
				
				// Add the reactant pattern data to the ruleData.
				ruleData.addReactantPatternData(reactantPatternData);
			
			} // Close reactantPatterns
			
			//Next, take care of the product patterns.
			Element productPatternListNode = reactionRule.getChild("ListOfProductPatterns", reactionRule.getNamespace());
			List<Element> productPatternList = productPatternListNode.getChildren();
			
			// For each product pattern
			for(Element productPattern : productPatternList)
			{
				// Create the intermediate data storage object
				RulePatternData productPatternData = new RulePatternData();
				
				// Get the list of molecules in the pattern
				Element productPatternMoleculeListNode = productPattern.getChild("ListofMolecules", productPattern.getNamespace());
				List<Element> productPatternMoleculeList = productPatternMoleculeListNode.getChildren();
				
				// For each of the productPatternMolecules
				for(Element productPatternMolecule : productPatternMoleculeList)
				{
				
					// Add the molecule to the productPatternData
					productPatternData.addMolecule(productPatternMolecule.getAttributeValue("name"));
					
					// Add the id->name infomration to the molecule registry.
					moleculeNameForID.put(productPatternMolecule.getAttributeValue("id"), productPatternMolecule.getAttributeValue("name"));
					
					// Molecules have components.  Get the list.
					Element productPatternMoleculeComponentListNode = productPatternMolecule.getChild("ListOfComponents", productPatternMolecule.getNamespace());
					List<Element> productPatternMoleculeComponentList = productPatternMoleculeComponentListNode.getChildren();
					
					// For each component.
					for(Element productPatternMoleculeComponent : productPatternMoleculeComponentList)
					{
						// Create the componentdata object 
						ComponentData component = new ComponentData(productPatternMoleculeComponent.getAttributeValue("name"));
						
						// Add it to the molecule
						productPatternData.addComponentToMolecule(component, productPatternMolecule.getAttributeValue("name"));
						
						// Add it to the registry
						System.out.println("Adding ComponentData for id: " + productPatternMoleculeComponent.getAttributeValue("id"));
						componentDataForID.put(productPatternMoleculeComponent.getAttributeValue("id"), component);
								   
						//There could be a state.
						if(productPatternMolecule.getAttributeValue("state") != null)
						{
							// Set the state in the data structure.
							productPatternData.setStateForComponentInMolecule(productPatternMoleculeComponent.getAttributeValue("state"), 
																			   productPatternMoleculeComponent.getAttributeValue("name"),
																			   productPatternMolecule.getAttributeValue("name"));
						} // Done with state
					} // done with productPatternMoleculeComponent
				} // Done with productPatternMolecule
				
				
				// Get the bonds in the productPatterns if they exist
				if(productPattern.getChild("ListofBonds", productPattern.getNamespace()) != null)
				{
					Element productPatternBondListNode = productPattern.getChild("ListofBonds", productPattern.getNamespace());
					List<Element> productPatternBondList = productPatternBondListNode.getChildren();
					
					// For each of the bonds in the productPatterns.
					for(Element productPatternBond : productPatternBondList)
					{
						// The ids of the components.
						String comp1 = productPatternBond.getAttributeValue("site1");
						String comp2 = productPatternBond.getAttributeValue("site2");
						
						// The ids are S#_M#_C#
						
						if(comp2.equals(""))
						{
							//WILDCARD, BITCHES!
							//TODO handle this later
							continue;
						}
						
						// the mole id is up to the second underscore
						String mole1id = comp1.substring(0, comp1.lastIndexOf("_"));
						String mole2id = comp2.substring(0, comp2.lastIndexOf("_"));
						
						// Get the names from the registries 
						String moleName1 = moleculeNameForID.get(mole1id);
						System.out.println("Trying to get component for id: " + comp1);
						String compName1 = componentDataForID.get(comp1).getComponent();
						String moleName2 = moleculeNameForID.get(mole2id);
						
						System.out.println("Trying to get component for id: " + comp2);				
	
						String compName2 = componentDataForID.get(comp2).getComponent();
						
						int compID1 = componentDataForID.get(comp1).getUniqueID();
						int compID2 = componentDataForID.get(comp2).getUniqueID();
						
						String state1 = componentDataForID.get(comp1).getState();
						String state2 = componentDataForID.get(comp1).getState();
						
						productPatternData.addbond(moleName1, compName1, compID1, state1, moleName2, compName2, compID2, state2);
					}	
				}
				
				
				// Add the product pattern data to the ruleData.
				ruleData.addProductPatternData(productPatternData);
			} //Done with productPatterns
			
			// There are laws about the rates at which a rule can occur
			Element rateLawNode = reactionRule.getChild("RateLaw", reactionRule.getNamespace());
			Element rateLawConstantListNode = rateLawNode.getChild("ListOfRateConstants", rateLawNode.getNamespace());
			List<Element> rateLawConstantList = rateLawConstantListNode.getChildren();
			
			// For each of the rate laws.
			for(Element rateLawConstant : rateLawConstantList)
			{
				// I think that this should be only 1 with the current setup of the parser. 
				ruleData.setRate(rateLawConstant.getAttributeValue("value"));
			}
			
			// There is a Map relation between the molecules and components in the reactants 
			// that also appear in the products.
			Element mapListNode = reactionRule.getChild("Map",reactionRule.getNamespace());
			List<Element> mapList = mapListNode.getChildren();
			
			// For each map
			for(Element map : mapList)
			{
				// I shouldn't need this....
			}
			
			// The operations are what change between the reactants and product.
			Element operationsListNode = reactionRule.getChild("ListOfOperations", reactionRule.getNamespace());
			List<Element> operationsList = operationsListNode.getChildren();
			
			// For each operation
			for(Element operation : operationsList)
			{
				
				// The ids of the components.
				String comp1 = operation.getAttributeValue("site1");
				String comp2 = operation.getAttributeValue("site2");
				

				if(comp2.equals(""))
				{
					//TODO WILDCARD, BITCHES!  Handled it later.
					continue;
				}
				
				// The ids are S#_M#_C#
				
				// the mole id is up to the second underscore
				String mole1id = comp1.substring(0, comp1.lastIndexOf("_"));
				String mole2id = comp2.substring(0, comp2.lastIndexOf("_"));
				
				// Get the names from the registries 
				String moleName1 = moleculeNameForID.get(mole1id);
				String compName1 = componentDataForID.get(comp1).getComponent();
				String moleName2 = moleculeNameForID.get(mole2id);
				System.out.println("Trying to get componentdata for id: " + comp2);
				String compName2 = componentDataForID.get(comp2).getComponent();
				
				String state1 = componentDataForID.get(comp1).getState();
				String state2 = componentDataForID.get(comp1).getState();
				
				int compID1 = componentDataForID.get(comp1).getUniqueID();
				int compID2 = componentDataForID.get(comp2).getUniqueID();
				
				int action;
				
				// If it is an Addbond operation
				if(operation.getValue().equals("AddBond"))
				{
					action = 1;
				}
				else
				{
					action = -1;
				}
				
				ruleData.addBondData(moleName1, compName1, compID1, state1, moleName2, compName2, compID2, state2, action);
				
			} // Done with operations
			
			m_builder.foundRule(ruleData);
		}// Done with reactionRules
	}// close method
	
	/**
	 * Handle the observables
	 * 
	 * Example:
	   <ListOfObservables>
		    <Observable id="O1" name="LynFree" type="Molecules">
		      <ListOfPatterns>
		      <Pattern id="O1_P1">  
		               <ListofMolecules>
		                 <Molecule id="O1_P1_M1" name="Lyn" >
		                  <ListOfComponents>
		                    <Component id="O1_P1_M1_C1"  name="U"  numberOfBonds="0" />
		                    <Component id="O1_P1_M1_C2"  name="SH2"  numberOfBonds="0" />
		                  </ListOfComponents>
		                 </Molecule>
		               </ListofMolecules>
		       
		        </Pattern>
		      </ListOfPatterns>
		    </Observable>
			...
		</ListOfObservables>
	 * 	
	 * @param observablesBlockNode
	 */
	public void readObservablesBlock(Element observablesBlockNode)
	{
		// Get the list
		List<Element> observablesList = observablesBlockNode.getChildren();
		
		// For each observable
		for(Element observable : observablesList)
		{
			m_builder.foundObservable(observable.getAttributeValue("id"),
									  observable.getAttributeValue("name"),
									  observable.getAttributeValue("type"));
			
			// Observables are made of patterns.
			Element observablePatternListNode = observable.getChild("ListOfPatterns", observable.getNamespace());
			List<Element> observablePatternList = observablePatternListNode.getChildren();
			
			// for each pattern
			for(Element observablePattern : observablePatternList)
			{
				m_builder.foundObservablePattern(observable.getAttributeValue("id"),
												observablePattern.getAttributeValue("id"));
				//patterns have molecules
				Element observablePatternMoleculeListNode = observablePattern.getChild("ListofMolecules", observablePattern.getNamespace());
				List<Element> observablePatternMoleculeList = observablePatternMoleculeListNode.getChildren();
				
				//For each observable pattern molecule
				for(Element observablePatternMolecule : observablePatternMoleculeList)
				{
					m_builder.foundObservablePatternMolecule(observable.getAttributeValue("id"),
															 observablePattern.getAttributeValue("id"),
															 observablePatternMolecule.getAttributeValue("id"),
															 observablePatternMolecule.getAttributeValue("name"));
					
					// molecules have components
					Element observablePatternMoleculeComponentListNode = observablePatternMolecule.getChild("ListOfComponents", observablePatternMolecule.getNamespace());
					List<Element> observablePatternMoleculeComponentList = observablePatternMoleculeComponentListNode.getChildren();
					
					// for each component
					for(Element observablePatternMoleculeComponent : observablePatternMoleculeComponentList)
					{
						m_builder.foundObservablePatternMoleculeComponent(observable.getAttributeValue("id"),
																		 observablePattern.getAttributeValue("id"),
																		 observablePatternMolecule.getAttributeValue("id"),
																		 observablePatternMoleculeComponent.getAttributeValue("id"),
																		 observablePatternMoleculeComponent.getAttributeValue("name"));
						
						// Some have states.
						if(observablePatternMoleculeComponent.getChild("state") != null)
						{
							m_builder.foundObservablePatternMoleculeComponentState(observable.getAttributeValue("id"),
									 observablePattern.getAttributeValue("id"),
									 observablePatternMolecule.getAttributeValue("id"),
									 observablePatternMoleculeComponent.getAttributeValue("id"),
									 observablePatternMoleculeComponent.getAttributeValue("state"));

						}
					}
				} // Done with observable pattern molecule
			}// done with observable pattern
		}// done with observable
	} // close method
	
	/**
	 * Handle the functions.
	 * TODO  I'll do this later.
	 */
	public void readFunctionsBlock(Element functionsBlockNode)
	{
		
	}
	
	/**
	 * Use JDOM to get the xml document from the ast.
	 * @param ast
	 * @return
	 */
	private Document getDocument(prog_return ast) 
	{
		SAXBuilder builder = new SAXBuilder();
        
		// Create the document
        try 
        {
        	StringReader reader = new StringReader(ast.toString());
			return builder.build(reader);
		} 
        catch (Exception e) 
        {
			e.printStackTrace();
		}
        
		return null;
	}
	
	public void setBuilder(ModelBuilderInterface builder)
	{
		m_builder = builder;
	}
}
