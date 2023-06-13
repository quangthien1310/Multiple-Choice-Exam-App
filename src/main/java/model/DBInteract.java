package model;

import javafx.scene.image.Image;
import org.apache.xmlbeans.impl.schema.StscChecker;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBInteract {

    private Connection conn;

    DBInteract() {
        conn = this.connect();
    }

    private Connection connect() {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:src/main/resources/data.db";
            conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("PRAGMA foreign_keys = ON");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void insertQuestion(Question q, String categoryTitle) {

        try {
            String sql = "INSERT INTO QUESTION(questionName,questionText,catID,questionImage) VALUES(?,?," +
                    "(SELECT catID FROM CATEGORY WHERE catTitle = ?),?)";
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, q.getQuestionName());
            pstmt.setString(2, q.getQuestionText());
            pstmt.setString(3,categoryTitle);
            pstmt.setBytes(4,DataInteract.changeImageToBytes(q.getQuestionImage()));
            pstmt.executeUpdate();

            sql = "INSERT INTO OPTIONS(questionName, optionLabel, optionData, optionImage) VALUES(?,?,?,?)";
            pstmt = conn.prepareStatement(sql);

            int n = q.getOptions().size();
            for (int i = 0;i<n;i++) {
                pstmt.setString(1, q.getQuestionName());
                pstmt.setString(2, String.valueOf(q.getOptions().get(i).charAt(0)));
                pstmt.setString(3, q.getOptions().get(i).substring(3));
                pstmt.setBytes(4, DataInteract.changeImageToBytes(q.getOptionImages().get(i)));
                pstmt.executeUpdate();
            }

            sql = "INSERT INTO ANSWER(questionName, optionLabel) VALUES(?,?)";
            pstmt = conn.prepareStatement(sql);
            for (Character c : q.getAns()) {
                pstmt.setString(1, q.getQuestionName());
                pstmt.setString(2, String.valueOf(c));
                pstmt.executeUpdate();
            }

            pstmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void createNewCategory(String parentCategoryTitle, String categoryID, String categoryTitle) {
        try {
            String sql = "INSERT INTO CATEGORY(catID, catTitle) VALUES(?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,categoryID);
            pstmt.setString(2,categoryTitle);
            pstmt.executeUpdate();

            if (parentCategoryTitle == null) return;
            sql = "INSERT INTO SUBCAT(catID, subCatID) VALUES((SELECT catID FROM CATEGORY WHERE catTitle = ?),?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,parentCategoryTitle);
            pstmt.setString(2,categoryID);
            pstmt.executeUpdate();

            pstmt.close();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public List<Question> getQuestionsBelongToCategory(String categoryTitle) {
        List<Question> questions = new ArrayList<>();
        try (Connection conn = this.connect()) {
            String sql = "SELECT questionName, questionText FROM QUESTION,CATEGORY WHERE catTitle = '" + categoryTitle
                    + "' AND CATEGORY.catID = QUESTION.catID";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Question q = new Question();
                String questionName = rs.getString("questionName");
                q.setQuestionName(questionName);
                q.setQuestionText(rs.getString(2));

                String opt = "SELECT optionLabel, optionData FROM OPTIONS WHERE questionName = '" + questionName +"'";
                Statement optStmt = conn.createStatement();
                ResultSet optRs = optStmt.executeQuery(opt);

                List<String> options = new ArrayList<>();
                while (optRs.next()) {
                    options.add(optRs.getString(1) + ": " + optRs.getString(2));
                }
                q.setOptions(options);

                String ans = "SELECT optionLabel FROM ANSWER WHERE questionName = '" + questionName + "'";
                Statement ansStmt = conn.createStatement();
                ResultSet ansRs = ansStmt.executeQuery(ans);
                List<Character> answers = new ArrayList<>();
                while (ansRs.next()) {
                    answers.add(ansRs.getString(1).charAt(0));
                }
                q.setAns(answers);

                questions.add(q);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return questions;
    }

    public List<Category> getAllCategories() {
        List<Category> cats = new ArrayList<>();
        try {
            String sql = "SELECT catID, catTitle FROM CATEGORY";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                cats.add(new Category(rs.getString(1),rs.getString(2)));
            }
            stmt.close();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return cats;
    }

    public List<Category> getSubCategoriesOf(String categoryTitle) {
        String sql = "SELECT subCatID,sub.catTitle FROM SUBCAT,CATEGORY parent,CATEGORY sub WHERE SUBCAT.catID = parent.catID " +
                "AND parent.catTitle = ? AND sub.catID = subCatID";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,categoryTitle);
            ResultSet rs = pstmt.executeQuery();

            List<Category> categories = new ArrayList<>();
            while (rs.next()) {
                Category cat = new Category(rs.getString(1),rs.getString(2));
                categories.add(cat);
            }
            pstmt.close();
            return categories;
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public String getCategoryID(String categoryTitle) {
        String sql = "SELECT catID FROM CATEGORY WHERE catTitle = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,categoryTitle);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString(1);
            else return null;
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void createNewQuiz(Quiz quiz) {
        String sql = "INSERT INTO QUIZ(quizName, quizDescription, timeLimit) VALUES(?,?,?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, quiz.getQuizName());
            pstmt.setString(2, quiz.getQuizDescription());
            pstmt.setInt(3, quiz.getTimeLimit());
            pstmt.executeUpdate();
            pstmt.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void addQuestionToQuiz(String quizName, String questionName) {
        String sql = "INSERT INTO QUIZ_QUESTION(quizID, questionName) VALUES((SELECT quizID FROM QUIZ WHERE quizName = ?),?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,quizName);
            pstmt.setString(2,questionName);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Question getQuestion(String questionName) {
        String sql = "SELECT questionText,questionImage FROM QUESTION WHERE questionName = ?";
        Question q = new Question();
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,questionName);
            ResultSet rs = pstmt.executeQuery();
            byte[] bytes;
            if (rs.next()) {
                q.setQuestionName(questionName);
                q.setQuestionText(rs.getString(1));
                bytes = rs.getBytes(2);
                if(bytes != null) q.setQuestionImage(DataInteract.changeBytesToImage(bytes));
                else q.setQuestionImage(null);
            }
            else return null;

            sql = "SELECT optionLabel, optionData, optionImage FROM OPTIONS WHERE questionName = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,questionName);
            rs = pstmt.executeQuery();
            List<String> options = new ArrayList<>();
            List< Image> optionImages = new ArrayList<>();
            while (rs.next()) {
                options.add(rs.getString(1) + ": " + rs.getString(2));
                bytes = rs.getBytes(3);
                if (bytes != null) optionImages.add(DataInteract.changeBytesToImage(bytes));
                else optionImages.add(null);
            }
            q.setOptions(options);
            q.setOptionImages(optionImages);

            sql = "SELECT optionLabel FROM ANSWER WHERE questionName = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,questionName);
            rs = pstmt.executeQuery();
            List<Character> ans = new ArrayList<>();
            while(rs.next()) {
                ans.add(rs.getString(1).charAt(0));
            }
            q.setAns(ans);

            return q;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public List<Question> getQuestionBelongToQuiz(String quizName) {
        String sql = "SELECT questionName FROM QUIZ, QUIZ_QUESTION WHERE quizName = ? AND QUIZ.quizID = QUIZ_QUESTION.quizID";
        List<Question> questions = new ArrayList<>();
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,quizName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                questions.add(getQuestion(rs.getString(1)));
            }
            pstmt.close();
            return questions;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void deleteQuestion(String questionName) {
        String sql = "DELETE FROM ANSWER WHERE questionName = '" + questionName + "'";
        try {
            //Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);

            sql = "DELETE FROM OPTIONS WHERE questionName = '" + questionName + "'";
            stmt.executeUpdate(sql);

            sql = "DELETE FROM QUESTION WHERE questionName = '" + questionName + "'";
            stmt.executeUpdate(sql);

            sql = "DELETE FROM QUIZ_QUESTION WHERE questionName = '" + questionName + "'";
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteCategory(String categoryTitle) {
        String sql = "SELECT catID FROM CATEGORY WHERE catTitle = '" + categoryTitle + "'";
        try {
            //Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {

                sql = "SELECT catTitle, subCatID FROM CATEGORY, SUBCAT WHERE SUBCAT.catID = '"
                        + rs.getString(1) + "' AND SUBCAT.subCatID = CATEGORY.catID";
                Statement stmt1 = conn.createStatement();
                ResultSet subRs = stmt1.executeQuery(sql);

                while (subRs.next()) {
                    sql = "DELETE FROM SUBCAT WHERE subCatID = '" + subRs.getString(2) + "'";
                    Statement stmt2 = conn.createStatement();
                    stmt2.executeUpdate(sql);
                    deleteCategory(subRs.getString(1));
                }

                sql = "SELECT questionName FROM QUESTION WHERE catID = '" + rs.getString(1) + "'";
                subRs = stmt1.executeQuery(sql);
                while (subRs.next()) {
                    deleteQuestion(subRs.getString(1));
                }


            }

            sql = "DELETE FROM CATEGORY WHERE catTitle = '" + categoryTitle + "'";
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteQuiz(String quizName) {
        String sql = "DELETE FROM QUIZ_QUESTION WHERE quizID IN (SELECT quizID FROM QUIZ WHERE quizName = ?)";
        try {
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,quizName);
            pstmt.executeUpdate();

            sql = "DELETE FROM QUIZ WHERE quizName = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,quizName);
            pstmt.executeUpdate();

            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<Quiz> getAllQuizzes() {
        String sql = "SELECT quizName, quizDescription, timeLimit FROM QUIZ";
        try {
            Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            List<Quiz> quizzes = new ArrayList<>();
            while (rs.next()) {
                Quiz quiz = new Quiz();
                quiz.setQuizName(rs.getString(1));
                quiz.setQuizDescription(rs.getString(2));
                quiz.setTimeLimit(rs.getInt(3));
                quizzes.add(quiz);
            }
            return quizzes;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public List<Category> getAllNonSubCategories() {
        String sql = "SELECT catID, catTitle FROM CATEGORY WHERE catID NOT IN (SELECT subCatID catID FROM SUBCAT)";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            List<Category> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(new Category(rs.getString(1),rs.getString(2)));
            }
            stmt.close();
            return categories;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void treeView(String categoryTitle, int level) {
        for (int i=0;i<level;i++) System.out.print(' ');
        System.out.println(categoryTitle);
        List<Category> subCategories = getSubCategoriesOf(categoryTitle);
        for (Category cat:subCategories) {
            treeView(cat.getCatTitle(),level+1);
        }
    }
}