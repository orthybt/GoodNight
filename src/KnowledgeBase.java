import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * The KnowledgeBase class represents a GUI application
 * to manage knowledge snippets tagged with one or more tags.
 */
public class KnowledgeBase {

	private JFrame frame;
	private JTextArea knowledgeTextArea;
	private JList<String> tagsList;
	private DefaultListModel<String> listModel;
	private Map<String, String> knowledgeMap; // Maps tags to knowledge snippets.
	private Stack<String> deletedTagsStack = new Stack<>(); // Stores recently deleted tags.

	/**
	 * Constructor initializes the GUI and loads knowledge if available.
	 */
	public KnowledgeBase() {
		knowledgeMap = new HashMap<>();
		listModel = new DefaultListModel<>();
		loadKnowledge();

		// Initialize GUI components
		initUI();
	}

	/**
	 * Initializes the main UI components.
	 */
	private void initUI() {
		frame = new JFrame("Knowledge Manager");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setSize(600, 400);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exitProcedure();
			}
		});

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel(new FlowLayout());
		knowledgeTextArea = new JTextArea();
		JScrollPane textAreaScrollPane = new JScrollPane(knowledgeTextArea);

		tagsList = new JList<>(listModel);
		JScrollPane listScrollPane = new JScrollPane(tagsList);
		tagsList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				String selectedTag = tagsList.getSelectedValue();
				knowledgeTextArea.setText(knowledgeMap.get(selectedTag));
			}
		});

		// Create buttons and add listeners
		JButton addButton = new JButton("Add");
		addButton.addActionListener(e -> addKnowledge());
		buttonPanel.add(addButton);

		JButton deleteButton = new JButton("Delete");
		deleteButton.addActionListener(e -> deleteTag());
		buttonPanel.add(deleteButton);

		JButton undoDeleteButton = new JButton("Undo Delete");
		undoDeleteButton.addActionListener(e -> undoDelete());
		buttonPanel.add(undoDeleteButton);

		panel.add(buttonPanel, BorderLayout.NORTH);
		panel.add(textAreaScrollPane, BorderLayout.CENTER);
		panel.add(listScrollPane, BorderLayout.EAST);

		frame.add(panel);
		frame.setVisible(true);
	}

	/**
	 * Prompts the user to select or add tags and saves the knowledge snippet.
	 */
	private void addKnowledge() {
		String knowledge = knowledgeTextArea.getText();
		if (knowledge.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Please enter knowledge.");
			return;
		}
		Set<String> tags = showTagSelectionDialog();
		for (String singleTag : tags) {
			singleTag = singleTag.trim();
			knowledgeMap.put(singleTag, knowledge);
			if (!listModel.contains(singleTag)) {
				listModel.addElement(singleTag);
			}
		}
	}

	/**
	 * Handles exit behavior, prompting the user to save changes.
	 */
	private void exitProcedure() {
		int choice = JOptionPane.showConfirmDialog(frame, "Do you want to save?", "Exit", JOptionPane.YES_NO_CANCEL_OPTION);
		if (choice == JOptionPane.YES_OPTION) {
			JFileChooser fileChooser = new JFileChooser();
			if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				saveToFile(file.getAbsolutePath());

				// Backup in the project directory
				saveToFile("knowledge_backup.txt");
				System.exit(0);
			}
		} else if (choice == JOptionPane.NO_OPTION) {
			System.exit(0);
		}
	}

	/**
	 * Saves the current knowledge map to a file.
	 *
	 * @param filePath The path of the file where knowledge should be saved.
	 */
	private void saveToFile(String filePath) {
		try (FileWriter writer = new FileWriter(filePath)) {
			for (Map.Entry<String, String> entry : knowledgeMap.entrySet()) {
				writer.write(entry.getKey() + " : " + entry.getValue() + "\n");
			}

			if (filePath.equals("knowledge_backup.txt")) {
				System.out.println("Backup saved at: " + new File(filePath).getAbsolutePath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads knowledge from a selected file or from the backup.
	 */
	private void loadKnowledge() {
		JFileChooser fileChooser = new JFileChooser();
		int choice = fileChooser.showOpenDialog(null);
		if (choice == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			loadFromFile(file.getAbsolutePath());
		} else {
			// Default to backup
			File backup = new File("knowledge_backup.txt");
			if (backup.exists()) {
				loadFromFile("knowledge_backup.txt");
			}
		}
	}

	/**
	 * Loads knowledge from the specified file.
	 *
	 * @param filePath The path of the file to load knowledge from.
	 */
	private void loadFromFile(String filePath) {
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(" : ", 2);
				if (parts.length >= 2) {
					String tag = parts[0].trim();
					String knowledge = parts[1].trim();

					knowledgeMap.put(tag, knowledge);
					listModel.addElement(tag);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shows a dialog where user can select existing tags or add new ones.
	 *
	 * @return A set of selected or newly added tags.
	 */
	private Set<String> showTagSelectionDialog() {
		JDialog dialog = new JDialog(frame, "Select or Add Tags", true);
		JPanel panel = new JPanel(new BorderLayout());

		JPanel newTagPanel = new JPanel();
		JTextField newTagField = new JTextField(20);
		newTagPanel.add(new JLabel("New Tags (comma-separated):"));
		newTagPanel.add(newTagField);
		panel.add(newTagPanel, BorderLayout.NORTH);

		JPanel checkBoxPanel = new JPanel();
		JCheckBox[] checkboxes = new JCheckBox[listModel.getSize()];
		checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

		for (int i = 0; i < listModel.getSize(); i++) {
			checkboxes[i] = new JCheckBox(listModel.getElementAt(i));
			checkBoxPanel.add(checkboxes[i]);
		}

		JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
		panel.add(scrollPane, BorderLayout.CENTER);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> dialog.dispose());
		panel.add(okButton, BorderLayout.SOUTH);

		dialog.add(panel);
		dialog.setSize(300, 200);
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);

		Set<String> selectedTags = new HashSet<>();
		for (JCheckBox checkbox : checkboxes) {
			if (checkbox.isSelected()) {
				selectedTags.add(checkbox.getText());
			}
		}
		if (!newTagField.getText().isEmpty()) {
			String[] newTags = newTagField.getText().split(",");
			Collections.addAll(selectedTags, newTags);
		}

		return selectedTags;
	}

	/**
	 * Deletes the selected tag from the list.
	 */
	private void deleteTag() {
		String selectedTag = tagsList.getSelectedValue();
		if (selectedTag != null) {
			deletedTagsStack.push(selectedTag); // Save tag for possible undo
			knowledgeMap.remove(selectedTag);
			listModel.removeElement(selectedTag);
		}
	}

	/**
	 * Restores the most recently deleted tag.
	 */
	private void undoDelete() {
		if (!deletedTagsStack.isEmpty()) {
			String tagToRestore = deletedTagsStack.pop();
			listModel.addElement(tagToRestore);
			knowledgeMap.put(tagToRestore, knowledgeTextArea.getText());
		}
	}

	/**
	 * Entry point for the application.
	 *
	 * @param args Command-line arguments (unused).
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new KnowledgeBase());
	}
}
