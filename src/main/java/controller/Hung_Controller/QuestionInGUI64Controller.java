package controller.Hung_Controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import model.Question;
import model.DataModel;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class QuestionInGUI64Controller implements Initializable {
    @FXML
    private Label mainQuesContent;
    @FXML
    private Label order;
    @FXML
    private Label subQuesContent;

    private List<Question> selectedQuestions;

    public void setOrder(int order) {
        this.order.setText(String.valueOf(order));
    }

    public void setQuestion(Question question) {
        this.mainQuesContent.setText(question.getQuestionName() + ": " + question.getQuestionText());
        this.subQuesContent.setText(question.getQuestionName() + ": " + question.getQuestionText());
    }

    @FXML
    public void deleteThisQuestion(MouseEvent event) {
        VBox questionList = (VBox) this.order.getScene().getRoot().lookup(".question-list");
        questionList.getChildren().remove(Integer.parseInt(this.order.getText()) - 1);
        selectedQuestions.remove(Integer.parseInt(this.order.getText()) - 1);
        Set<Node> orderNumbers = questionList.lookupAll(".order-num");
        int j = 1;
        for (Node orderNum : orderNumbers) {
            Label label = (Label) orderNum;
            label.setText(String.valueOf(j));
            j++;
        }
        DataModel.getInstance().getQuantityLabel().setText(String.valueOf(j - 1));
        DataModel.getInstance().getTotalMark().setText("Total of marks: " + String.valueOf(j - 1) + ".00");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        selectedQuestions = DataModel.getInstance().getSelectedQuestions();
    }
}
