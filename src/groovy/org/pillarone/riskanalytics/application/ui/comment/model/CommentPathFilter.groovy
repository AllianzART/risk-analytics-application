package org.pillarone.riskanalytics.application.ui.comment.model

import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.core.parameterization.validation.ParameterValidation
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.Comment

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
@CompileStatic
class CommentPathFilter implements CommentFilter {
    //comment path
    String path
    //path comment looks different as error path
    //path comment started with modelName:....
    String errorPath

    public CommentPathFilter(String path) {
        this.path = path
        int index = path.indexOf(":")
        if (index != -1 && index < path.length() - 1) {
            errorPath = path.substring(index + 1, path.length())
        }
    }

    boolean accept(Comment comment) {
        return path.equals(comment.path)
    }

    boolean accept(ParameterValidation error) {
        return errorPath?.equals(error.path)
    }

}
