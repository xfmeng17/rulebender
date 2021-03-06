package rulebender.editors.dat.model;

import java.io.File;
import java.util.List;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import rulebender.results.data.DATFileData;

/**
 * 
 * File Node
 * 
 */
public class FileNode extends TreeNode {
	private File fFile; // actual data object
	private FileData fData;
	private String fileName;
	private String filePath;

	private static final Image m_bngImage = AbstractUIPlugin.imageDescriptorFromPlugin ("rulebender","/icons/views/CMap.png").createImage();
	private static final Image m_resultsImage = AbstractUIPlugin.imageDescriptorFromPlugin ("rulebender","/icons/views/Results.png").createImage();
	private static final Image m_defaultImage = AbstractUIPlugin.imageDescriptorFromPlugin ("rulebender","/icons/views/file_obj.png").createImage();
	
	public FileNode(ITreeNode parent, File file) {
		super("FileNode", parent);
		fFile = file;
		if (fFile != null) {
			String filename = fFile.getName();
			setName(filename);
			String filepath = fFile.getPath();
			setPath(filepath);
		}
	}

	/**
	 * Set display name of files based on their types.
	 * 
	 * @param filename
	 *            real name of file
	 */
	public void setName(String filename) {
		String suffix = null;
		if (filename.endsWith(".net"))
			suffix = "NET";
		if (filename.endsWith(".cdat"))
			suffix = "CDAT";
		if (filename.endsWith(".gdat"))
			suffix = "GDAT";
		if (filename.endsWith(".scan"))
			suffix = "SCAN";
		if (filename.endsWith(".bngl"))
			suffix = "BNGL";
		if (filename.endsWith(".log"))
			suffix = "LOG";
		if (filename.endsWith(".pl"))
			suffix = "PL";
		if (filename.endsWith(".m"))
			suffix = "M";
		if (filename.endsWith(".xml"))
			suffix = "XML";
		if (filename.endsWith(".rxn"))
			suffix = "RXN";
		if (filename.endsWith(".cfg"))
			suffix = "CFG";
		
		if(suffix != null)
		{
			this.fileName = (suffix + ": " + filename);
		}
		else
		{
			this.fileName = (filename);
		}
	}

	/**
	 * @return the display name of file
	 */
	public String getName() {
		return fileName;
	}

	/**
	 * 
	 * @return file path
	 */
	public String getPath() {
		return this.filePath;
	}

	/**
	 * 
	 * @param filepath
	 */
	public void setPath(String filepath) {
		this.filePath = filepath;
	}

	/**
	 * @return file image
	 */
	public Image getImage() 
	{
		if (fileName.endsWith(".cdat") ||
			fileName.endsWith(".gdat") ||
			fileName.endsWith(".scan"))
		{
			return m_resultsImage;
		}
			
			
		else if (fileName.endsWith(".net") ||
			fileName.endsWith(".bngl"))
		{
			return m_bngImage;
		}
		
		//fileName.endsWith(".log") || fileName.endsWith(".m") ||  fileName.endsWith(".xml"))
		else
		{
			return m_defaultImage;
		}
	}

	protected void createChildren(List children) {
	}

	public boolean hasChildren() {
		return false;
	}

	/**
	 * 
	 * @return FileData object
	 */
	public FileData getFileData() {
		// different FileData for different format
		if (fData == null) {
			String filename = fFile.getName();
			if (filename.endsWith(".net") || filename.endsWith(".bngl")) {
				fData = new NETFileData(this.fFile);
			} else if (filename.endsWith(".cdat") || filename.endsWith(".gdat")
					|| filename.endsWith(".scan")) {
				fData = new DATFileData(this.fFile);
			} 

		}
		return fData;
	}

	/**
	 * Reset the fileData after refresh the file_tv tree
	 * 
	 * @param fData
	 *            FileData object
	 */
	public void setfData(FileData fData) {
		this.fData = fData;
	}

	public boolean equals(Object other) {
		if (!(other instanceof FileNode)) {
			return false;
		}

		if (((FileNode) other).getPath().equals(this.getPath())) {
			return true;
		}

		return false;
	}

	public int hashCode() {
		String path = this.getPath();
		int hashcode = 0;
		for (int i = 0; i < path.length(); i++) {
			Character ch = path.charAt(i);
			hashcode += ch.hashCode();
		}
		return hashcode;
	}
}
