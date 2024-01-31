package com.algaworks.junit.blog.negocio;

import com.algaworks.junit.blog.modelo.Editor;
import com.algaworks.junit.blog.modelo.Ganhos;
import com.algaworks.junit.blog.modelo.Post;

import java.math.BigDecimal;

public class PostTestData {

    private PostTestData() {

    }

    public static Post.Builder umPostNovo() {
        Editor autor = EditorTestData.umEditorExistente().build();

        return Post.builder()
                .comTitulo("Título")
                .comConteudo("Conteúdo")
                .comAutor(autor)
                .comPago(false)
                .comPublicado(false);
    }

    public  static Post.Builder umPostExistente() {
        Ganhos ganhos = new Ganhos(BigDecimal.TEN, 4, BigDecimal.valueOf(40));

        return umPostNovo()
                .comId(1L)
                .comSlug("slug")
                .comGanhos(ganhos);
    }

    public  static Post.Builder umPostExistentePago() {
        return umPostExistente().comPago(true);
    }

    public  static Post.Builder umPostExistentePublicado() {
        return umPostExistente().comPublicado(true);
    }

}
