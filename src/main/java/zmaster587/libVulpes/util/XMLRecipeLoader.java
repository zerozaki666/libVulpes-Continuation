package zmaster587.libVulpes.util;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.interfaces.IRecipe;
import zmaster587.libVulpes.recipe.NumberedOreDictStack;
import zmaster587.libVulpes.recipe.RecipesMachine;
import zmaster587.libVulpes.tile.TileEntityMachine;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class XMLRecipeLoader {

	Document doc;
	String fileName;

	public XMLRecipeLoader() {
		doc = null;
		fileName = "";
	}

	public boolean loadFile(File xmlFile) throws IOException {
		DocumentBuilder docBuilder;
		doc = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			docBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LibVulpes.logger.warning("Unable to configure XML recipe parser: " + e.getMessage());
			return false;
		}

		try {
			doc = docBuilder.parse(xmlFile);
		} catch (SAXException e) {
			e.printStackTrace();
			return false;
		}

		fileName = xmlFile.getAbsolutePath();

		return true;
	}

	public void registerRecipes(Class<? extends TileEntityMachine> clazz) {
		if(clazz == null) {
			LibVulpes.logger.warning("Cannot register XML recipes without a machine class");
			return;
		}
		if(doc == null) {
			LibVulpes.logger.warning("Cannot register recipes before an XML document is loaded");
			return;
		}
		Node masterNode = doc.getElementsByTagName("Recipes").item(0);
		int recipeNum = 1;
		if(masterNode == null) {
			LibVulpes.logger.warning("Missing \"Recipes\" root node in " + fileName);
			return;
		}

		if(masterNode.hasAttributes()) {
			Node defaultNode = masterNode.getAttributes().getNamedItem("useDefault");
			if(defaultNode != null && defaultNode.getNodeValue().equalsIgnoreCase("false")
					&& RecipesMachine.getInstance().getRecipes(clazz) != null)
				RecipesMachine.getInstance().clearRecipes(clazz);
		}

		masterNode = masterNode.getFirstChild();
		
		while(masterNode != null) {
			try {
				int time = 200, energy = 0;
				if(masterNode.getNodeType() != Node.ELEMENT_NODE) {
					masterNode = masterNode.getNextSibling();
					continue;
				}
				if(!masterNode.getNodeName().equals("Recipe")) {
					LibVulpes.logger.warning("Expected \"Recipe\" Node in " + fileName + ", found " + masterNode.getNodeName() + "!  Skipping.");
					masterNode = masterNode.getNextSibling();
					continue;
				}

				Node inputNode = null, outputNode = null;
				for(int i = 0; i < masterNode.getChildNodes().getLength(); i++) {
					Node node = masterNode.getChildNodes().item(i);
					if(node.getNodeName().equals("input")) {
						inputNode = node;
					}
					else if(node.getNodeName().equals("output")) {
						outputNode = node;
					}
				}

				if(outputNode == null) {
					masterNode = masterNode.getNextSibling();
					LibVulpes.logger.warning("Missing \"output\" Node in recipe " + recipeNum + " in " + fileName + "!  Skipping.");
					recipeNum++;
					continue;
				}
				if(inputNode == null) {
					masterNode = masterNode.getNextSibling();
					LibVulpes.logger.warning("Missing \"input\" Node in recipe " + recipeNum + " in " + fileName + "!  Skipping.");
					recipeNum++;
					continue;
				}

				List<Object> inputList = new LinkedList<>();
				boolean invalidInput = false;

				for(int i = 0; i < inputNode.getChildNodes().getLength(); i++) {
					Node node = inputNode.getChildNodes().item(i);
					if(node.getNodeType() != Node.ELEMENT_NODE) continue;

					Object obj = parseItemType(node, false);
					if(obj == null) {
						LibVulpes.logger.warning("Invalid item \"input\" (" + node.getNodeName() + " " + node.getTextContent() + ") in recipe " + recipeNum + " in " + fileName + "!  Skipping.");
						invalidInput = true;
					}
					else
						inputList.add(obj);
				}

				List<Object> outputList = new LinkedList<>();
				boolean invalidOutput = false;

				for(int i = 0; i < outputNode.getChildNodes().getLength(); i++) {
					Node node = outputNode.getChildNodes().item(i);

					if(node.getNodeType() != Node.ELEMENT_NODE) continue;

					Object obj = parseItemType(node, true);
					if(obj == null) {
						LibVulpes.logger.warning("Invalid item \"output\" (" + node.getNodeName() + " " + node.getTextContent() + ") in recipe " + recipeNum + " in " + fileName + "!  Skipping.");
						invalidOutput = true;
					}
					else
						outputList.add(obj);
				}

				if(masterNode.hasAttributes()) {
					Node node = masterNode.getAttributes().getNamedItem("timeRequired");
					if(node != null && !node.getNodeValue().isEmpty()) {
						try {
							time = Integer.parseInt(node.getNodeValue());
						} catch (NumberFormatException e) {
							LibVulpes.logger.warning("Recipe " + recipeNum + " has no time value");
						}
					}

					node = masterNode.getAttributes().getNamedItem("power");
					if(node != null && !node.getNodeValue().isEmpty()) {
						try {
							energy = Integer.parseInt(node.getNodeValue());
						} catch (NumberFormatException e) {
							LibVulpes.logger.warning("Recipe " + recipeNum + " has no power value");
						}
					}
				}
				else {
					LibVulpes.logger.info("Recipe " + recipeNum + " has no time or power consumption");
				}

				if(invalidInput || inputList.isEmpty()) {
					LibVulpes.logger.warning("Input list is invalid or empty in recipe " + recipeNum + "; recipe skipped");
				}
				else if(invalidOutput || outputList.isEmpty()) {
					LibVulpes.logger.warning("Output list is invalid or empty in recipe " + recipeNum + "; recipe skipped");
				}
				else {
					RecipesMachine.getInstance().addRecipe(clazz, outputList, time, energy, inputList);
				}
			} catch (Exception e) {
				LibVulpes.logger.warning("Recipe entry #" + recipeNum + " load failed for '" + clazz.getCanonicalName() + "': " + e.getMessage());
			}

			recipeNum++;
			masterNode = masterNode.getNextSibling();
		}
	}

	public Object parseItemType(Node node, boolean output) {
        switch (node.getNodeName()) {
            case "itemStack": {
                String text = node.getTextContent().trim();
                String[] splitStr = text.contains(";")
						? text.split(";", 4)
						: text.split("\\s+");
                String name = splitStr[0].trim();
                int size = 1;
                int meta = 0;
                String nbtText = "";
				try {
					if(splitStr.length > 1)
						size = Integer.parseInt(splitStr[1].trim());
					if(splitStr.length > 2)
						meta = Integer.parseInt(splitStr[2].trim());
					if(splitStr.length > 3)
						nbtText = splitStr[3].trim();
				}
				catch(NumberFormatException e) {
					LibVulpes.logger.warning("Invalid item count or metadata in XML entry: " + text);
					return null;
				}

				if(size <= 0) {
					LibVulpes.logger.warning("Item count must be positive in XML entry: " + text);
					return null;
				}

				ItemStack stack = createItemStack(name, size, meta);
				if(stack == null)
					return null;
				if(!nbtText.isEmpty()) {
					try {
						NBTBase nbt = JsonToNBT.func_150315_a(nbtText);
						if(!(nbt instanceof NBTTagCompound)) {
							LibVulpes.logger.warning("Item NBT must be a compound in XML entry: " + text);
							return null;
						}
						stack.setTagCompound((NBTTagCompound)nbt);
					}
					catch(NBTException e) {
						LibVulpes.logger.warning("Invalid item NBT in XML entry: " + text + " (" + e.getMessage() + ")");
						return null;
					}
				}
				return stack;
            }
            case "oreDict": {
                String text = node.getTextContent().trim();
                String[] splitStr = text.contains(";")
						? text.split(";", 2)
						: text.split("\\s+", 2);
                String name = splitStr[0].trim();
				List<ItemStack> ores = OreDictionary.getOres(name);
				if(ores.isEmpty())
					return null;

				int number = 1;
				if(splitStr.length > 1) {
					try {
						number = Integer.parseInt(splitStr[1].trim());
					}
					catch(NumberFormatException e) {
						LibVulpes.logger.warning("Invalid Ore Dictionary count in XML entry: " + text);
						return null;
					}
				}
				if(number <= 0)
					return null;

				if(output) {
					for(ItemStack oreDict : ores) {
						if(oreDict != null && oreDict.getItem() != null) {
							ItemStack outputStack = oreDict.copy();
							outputStack.stackSize = number;
							return outputStack;
						}
					}
					return null;
				}
				return new NumberedOreDictStack(name, number);
            }
            case "fluidStack": {

                String text = node.getTextContent().trim();
                String[] splitStr = text.contains(";")
						? text.split(";", 2)
						: text.split("\\s+", 2);

                Fluid fluid;
                if ((fluid = FluidRegistry.getFluid(splitStr[0].trim())) != null) {
                    int amount = 1000;
                    if (splitStr.length > 1) {
                        try {
                            amount = Integer.parseInt(splitStr[1].trim());
                        } catch (NumberFormatException e) {
							LibVulpes.logger.warning("Invalid fluid amount in XML entry: " + text);
							return null;
                        }
                    }
					if(amount <= 0)
						return null;
                    return new FluidStack(fluid, amount);
                }
                break;
            }
        }

		return null;
	}

	private static ItemStack createItemStack(String name, int size, int meta) {
		Block block = Block.getBlockFromName(name);
		if(block != null)
			return new ItemStack(block, size, meta);

		Item item = (Item)Item.itemRegistry.getObject(name);
		if(item == null) {
			try {
				item = Item.getItemById(Integer.parseInt(name));
			}
			catch(NumberFormatException ignored) {
				return null;
			}
		}
		return item == null ? null : new ItemStack(item, size, meta);
	}

	public static String writeRecipe(IRecipe recipe) {
		if(recipe == null)
			throw new IllegalArgumentException("recipe cannot be null");

		int index = 0;
		StringBuilder string = new StringBuilder("\t<Recipe timeRequired=\"" + recipe.getTime() + "\" power=\"" + recipe.getPower() + "\">\n" +
                "\t\t<input>\n");
		for(List<ItemStack> stackList : recipe.getIngredients()) {
			String oreStr = recipe.getOreDictString(index++);
			ItemStack stack = getFirstValidStack(stackList);
			if(stack != null) {
				if(oreStr != null) {
					string.append("\t\t\t<oreDict>")
							.append(escapeXml(oreStr))
							.append(stack.stackSize > 1 ? (";" + stack.stackSize) : "")
							.append("</oreDict>\n");
				}
				else {
					string.append("\t\t\t<itemStack>")
							.append(escapeXml(writeItemStack(stack)))
							.append("</itemStack>\n");
				}
			}
		}
		for(FluidStack stack : recipe.getFluidIngredients()) {
			String fluid = writeFluidStack(stack);
			if(fluid != null)
				string.append("\t\t\t<fluidStack>").append(escapeXml(fluid)).append("</fluidStack>\n");
		}
		string.append("\t\t</input>\n\t\t<output>\n");

		for(ItemStack stack : recipe.getOutput()) {
			if(stack != null && stack.getItem() != null && stack.stackSize > 0)
				string.append("\t\t\t<itemStack>")
						.append(escapeXml(writeItemStack(stack)))
						.append("</itemStack>\n");
		}

		for(FluidStack stack : recipe.getFluidOutputs()) {
			String fluid = writeFluidStack(stack);
			if(fluid != null)
				string.append("\t\t\t<fluidStack>").append(escapeXml(fluid)).append("</fluidStack>\n");
		}

		string.append("\t\t</output>\n\t</Recipe>");

		return string.toString();
	}

	private static ItemStack getFirstValidStack(List<ItemStack> stacks) {
		if(stacks != null) {
			for(ItemStack stack : stacks) {
				if(stack != null && stack.getItem() != null && stack.stackSize > 0)
					return stack;
			}
		}
		return null;
	}

	private static String writeFluidStack(FluidStack stack) {
		if(stack == null || stack.getFluid() == null || stack.amount <= 0)
			return null;
		String name = FluidRegistry.getDefaultFluidName(stack.getFluid());
		return name == null || name.isEmpty() ? null : name + ";" + stack.amount;
	}

	private static String writeItemStack(ItemStack stack) {
		StringBuilder value = new StringBuilder(stack.getItem().delegate.name());
		boolean hasNbt = stack.hasTagCompound();
		if(stack.stackSize != 1 || stack.getItemDamage() != 0 || hasNbt)
			value.append(';').append(stack.stackSize);
		if(stack.getItemDamage() != 0 || hasNbt)
			value.append(';').append(stack.getItemDamage());
		if(hasNbt)
			value.append(';').append(stack.getTagCompound());
		return value.toString();
	}

	private static String escapeXml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
