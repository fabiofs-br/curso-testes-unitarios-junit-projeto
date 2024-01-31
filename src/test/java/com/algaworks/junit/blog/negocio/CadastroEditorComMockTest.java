package com.algaworks.junit.blog.negocio;

import com.algaworks.junit.blog.armazenamento.ArmazenamentoEditor;
import com.algaworks.junit.blog.exception.EditorNaoEncontradoException;
import com.algaworks.junit.blog.exception.RegraNegocioException;
import com.algaworks.junit.blog.modelo.Editor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
public class CadastroEditorComMockTest {

    @Captor
    ArgumentCaptor<Mensagem> mensagemArgumentCaptor;

    @Mock
    ArmazenamentoEditor armazenamentoEditor;

    @Mock
    GerenciadorEnvioEmail gerenciadorEnvioEmail;

    @InjectMocks
    CadastroEditor cadastroEditor;

    @Nested
    class CadastroComEditorValido {
        @Spy
        Editor editor = EditorTestData.umEditorNovo().build();

        @BeforeEach
        void init() {
            when(armazenamentoEditor.salvar(any(Editor.class))).thenAnswer(invocation -> {
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
            verify(armazenamentoEditor, times(1)).salvar(eq(editor));
        }

        @Test
        void Dado_um_editor_valido_Quando_criar_e_lancar_exception_ao_salvar_Entao_nao_deve_enviar_email() {
            when(armazenamentoEditor.salvar(editor)).thenThrow(new RuntimeException());
            assertAll("Não deve enviar e-mail, quando lançar exception do armazenamento",
                    () -> assertThrows(RuntimeException.class, () -> cadastroEditor.criar(editor)),
                    () -> verify(gerenciadorEnvioEmail, never()).enviarEmail(any())
            );
        }

        @Test
        void Dado_um_editor_valido_Quando_cadastrar_Entao_deve_enviar_email_com_destino_ao_editor() {
            Editor editorSalvo = cadastroEditor.criar(editor);

            verify(gerenciadorEnvioEmail).enviarEmail(mensagemArgumentCaptor.capture());

            Mensagem mensagem = mensagemArgumentCaptor.getValue();

            assertEquals(editorSalvo.getEmail(), mensagem.getDestinatario());
        }

        @Test
        void Dado_um_editor_valido_Quando_cadastrar_Entao_deve_verificar_o_email() {
            cadastroEditor.criar(editor);
            verify(editor, atLeast(1)).getEmail();

        }

        @Test
        void Dado_um_editor_com_email_existente_Quando_cadastrar_Entao_deve_lancar_exception() {
            when(armazenamentoEditor.encontrarPorEmail("alex@email.com"))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(editor));

            Editor editorComEmailExistente = EditorTestData.umEditorNovo().build();
            cadastroEditor.criar(editor);
            assertThrows(RegraNegocioException.class, () -> cadastroEditor.criar(editorComEmailExistente));
        }

        @Test
        void Dado_um_editor_valido_Quando_cadastrar_Entao_deve_enviar_email_apos_salvar() {
            cadastroEditor.criar(editor);

            InOrder inOrder = inOrder(armazenamentoEditor, gerenciadorEnvioEmail);
            inOrder.verify(armazenamentoEditor, times(1)).salvar(editor);
            inOrder.verify(gerenciadorEnvioEmail, times(1)).enviarEmail(any(Mensagem.class));
        }
    }

    @Nested
    class CadastroComEditorNull {
        @Test
        void Dado_um_editor_null_Quando_cadastrar_Entao_deve_lancar_exception() {
            Assertions.assertThrows(NullPointerException.class, () -> cadastroEditor.criar(null));
            verify(armazenamentoEditor, never()).salvar(any());
            verify(gerenciadorEnvioEmail, never()).enviarEmail(any());
        }
    }

    @Nested
    class EdicaoComEditorValido {
        @Spy
        Editor editor = EditorTestData.umEditorExistente().build();

        @BeforeEach
        void init() {
            when(armazenamentoEditor.salvar(editor)).thenAnswer(invocacao -> invocacao.getArgument(0, Editor.class));
            when(armazenamentoEditor.encontrarPorId(1L)).thenReturn(Optional.of(editor));
        }

        @Test
        void Dado_um_editor_valido_Quando_editar_Entao_deve_alterar_editor_salvo() {
            Editor editorAtualizado = EditorTestData.umEditorExistente()
                    .comNome("Alex Silva")
                    .comEmail("alex.silva@email.com")
                    .build();
            cadastroEditor.editar(editorAtualizado);
            verify(editor, times(1)).atualizarComDados(editorAtualizado);

            InOrder inOrder = inOrder(editor, armazenamentoEditor);
            inOrder.verify(editor).atualizarComDados(editorAtualizado);
            inOrder.verify(armazenamentoEditor).salvar(editor);
        }
    }

    @Nested
    class EdicaoComEditorInexistente {

        Editor editor = EditorTestData.umEditorComIdInexistente().build();

        @BeforeEach
        void init() {
            Mockito.when(armazenamentoEditor.encontrarPorId(99L)).thenReturn(Optional.empty());
        }

        @Test
        void Dado_um_editor_que_nao_exista_Quando_editar_Entao_deve_lancar_exception() {
            assertThrows(EditorNaoEncontradoException.class, () -> cadastroEditor.editar(editor));
            verify(armazenamentoEditor, never()).salvar(Mockito.any(Editor.class));
        }
    }
}

