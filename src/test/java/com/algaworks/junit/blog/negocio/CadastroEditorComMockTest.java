package com.algaworks.junit.blog.negocio;

import com.algaworks.junit.blog.armazenamento.ArmazenamentoEditor;
import com.algaworks.junit.blog.exception.RegraNegocioException;
import com.algaworks.junit.blog.modelo.Editor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
public class CadastroEditorComMockTest {


    @Spy
    Editor editor = new Editor(null, "Alex", "alex@email.com", BigDecimal.TEN, true);

    @Captor
    ArgumentCaptor<Mensagem> mensagemArgumentCaptor;

    @Mock
    ArmazenamentoEditor armazenamentoEditor;

    @Mock
    GerenciadorEnvioEmail gerenciadorEnvioEmail;

    @InjectMocks
    CadastroEditor cadastroEditor;

    @BeforeEach
    void init() {
        Mockito.when(armazenamentoEditor.salvar(Mockito.any(Editor.class))).thenAnswer(invocation -> {
            Editor editorPassado = invocation.getArgument(0, Editor.class);
            editorPassado.setId(1L);
            return editorPassado;
        });
    }

    @Test
    void Dado_um_editor_valido_Quando_criar_Entao_deve_retornar_um_id_de_cadastro() {
        Editor editorSalvo = cadastroEditor.criar(editor);
        assertEquals(1L, editorSalvo.getId());
    }

    @Test
    void Dado_um_editor_valido_Quando_criar_Entao_deve_chamar_metodo_salvar_do_armazenamento() {
        cadastroEditor.criar(editor);
        Mockito.verify(armazenamentoEditor, Mockito.times(1)).salvar(Mockito.eq(editor));
    }

    @Test
    void Dado_um_editor_valido_Quando_criar_e_lancar_exception_ao_salvar_Entao_nao_deve_enviar_email() {
        Mockito.when(armazenamentoEditor.salvar(editor)).thenThrow(new RuntimeException());
        assertAll("Não deve enviar e-mail, quando lançar exception do armazenamento",
                () -> assertThrows(RuntimeException.class, () -> cadastroEditor.criar(editor)),
                () -> Mockito.verify(gerenciadorEnvioEmail, Mockito.never()).enviarEmail(Mockito.any())
        );
    }

    @Test
    void Dado_um_editor_valido_Quando_cadastrar_Entao_deve_enviar_email_com_destino_ao_editor() {
        Editor editorSalvo = cadastroEditor.criar(editor);

        Mockito.verify(gerenciadorEnvioEmail).enviarEmail(mensagemArgumentCaptor.capture());

        Mensagem mensagem = mensagemArgumentCaptor.getValue();

        assertEquals(editorSalvo.getEmail(), mensagem.getDestinatario());
    }

    @Test
    void Dado_um_editor_valido_Quando_cadastrar_Entao_deve_verificar_o_email() {
        cadastroEditor.criar(editor);
        Mockito.verify(editor, Mockito.atLeast(1)).getEmail();

    }

    @Test
    void Dado_um_editor_com_email_existente_Quando_cadastrar_Entao_deve_lancar_exception() {
        Mockito.when(armazenamentoEditor.encontrarPorEmail("alex@email.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(editor));

        Editor editorComEmailExistente = new Editor(null, "Alex", "alex@email.com", BigDecimal.TEN, true);
        cadastroEditor.criar(editor);
        assertThrows(RegraNegocioException.class, () -> cadastroEditor.criar(editorComEmailExistente));
    }

}
