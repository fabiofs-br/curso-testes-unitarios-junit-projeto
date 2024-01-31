package com.algaworks.junit.blog.negocio;

import com.algaworks.junit.blog.armazenamento.ArmazenamentoPost;
import com.algaworks.junit.blog.exception.PostNaoEncontradoException;
import com.algaworks.junit.blog.exception.RegraNegocioException;
import com.algaworks.junit.blog.modelo.Ganhos;
import com.algaworks.junit.blog.modelo.Notificacao;
import com.algaworks.junit.blog.modelo.Post;
import com.algaworks.junit.blog.utilidade.ConversorSlug;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Cadastro de Post")
@ExtendWith(MockitoExtension.class)
class CadastroPostTest {

    @Mock
    ArmazenamentoPost armazenamentoPost;

    @Mock
    CalculadoraGanhos calculadoraGanhos;

    @Mock
    GerenciadorNotificacao gerenciadorNotificacao;

    @InjectMocks
    CadastroPost cadastroPost;

    @Captor
    ArgumentCaptor<Notificacao> notificacaoArgumentCaptor;

    Ganhos ganhos = new Ganhos(BigDecimal.TEN, 4, BigDecimal.valueOf(40));

    @Nested
    @DisplayName("Dado um post válido")
    class DadoUmPostValido {

        @Nested
        @DisplayName("Quando criar")
        class QuandoCriar {

            @Spy
            Post post = PostTestData.umPostNovo().build();;

            @BeforeEach
            void beforeEach() {
                when(armazenamentoPost.salvar(any(Post.class))).thenAnswer(invocation -> {
                    Post postPassado = invocation.getArgument(0, Post.class);
                    postPassado.setId(1L);
                    return postPassado;
                });
            }

            @Test
            @DisplayName("Então deve salvar")
            void entaoDeveSalvar() {
                cadastroPost.criar(post);
                verify(armazenamentoPost, times(1)).salvar(post);
            }

            @Test
            @DisplayName("Então deve retornar um ID valido")
            void entaoDeveRetornarUmIdValido() {
                Post postCriado = cadastroPost.criar(post);
                assertEquals(1L, postCriado.getId());
            }

            @Test
            @DisplayName("Então deve retornar post com slug")
            void entaoDeveRetornarPostComSlug() {
                Post postSalvo = cadastroPost.criar(post);

                verify(post, times(1)).setSlug(anyString());
                assertNotNull(postSalvo.getSlug());
            }

            @Test
            @DisplayName("Então deve gerar slug antes de salvar")
            void entaoDeveGerarSlugAntesDeSalvar() {
                try (MockedStatic<ConversorSlug> conversorSlug = mockStatic(ConversorSlug.class)) {
                    cadastroPost.criar(post);

                    InOrder inOrder = inOrder(ConversorSlug.class, armazenamentoPost);
                    inOrder.verify(conversorSlug, () -> ConversorSlug.converterJuntoComCodigo(post.getTitulo()), times(1));
                    inOrder.verify(armazenamentoPost, times(1)).salvar(post);
                }
            }

            @Test
            @DisplayName("Então deve retornar post com ganhos")
            void entaoDeveRetornarPostComGanhos() {
                when(calculadoraGanhos.calcular(post)).thenReturn(ganhos);

                Post postSalvo = cadastroPost.criar(post);

                verify(post, times(1)).setGanhos(any(Ganhos.class));
                assertNotNull(postSalvo.getGanhos());
            }

            @Test
            @DisplayName("Então deve calcular ganhos antes de salvar")
            void entaoDeveCalcularGanhosAntesDeSalvar() {
                cadastroPost.criar(post);

                InOrder inOrder = inOrder(calculadoraGanhos,armazenamentoPost);
                inOrder.verify(calculadoraGanhos, times(1)).calcular(post);
                inOrder.verify(armazenamentoPost, times(1)).salvar(post);
            }

            @Test
            @DisplayName("Então deve enviar notificação após salvar")
            void entaoDeveEnviarNotificacaoAposSalvar() {
                cadastroPost.criar(post);

                InOrder inOrder = inOrder(armazenamentoPost, gerenciadorNotificacao);
                inOrder.verify(armazenamentoPost, times(1)).salvar(post);
                inOrder.verify(gerenciadorNotificacao, times(1)).enviar(any(Notificacao.class));
            }

            @Test
            @DisplayName("Então deve gerar notificação com título do post")
            void entaoDeveGerarNotificacaoComTituloDoPost () {
                cadastroPost.criar(post);

                verify(gerenciadorNotificacao).enviar(notificacaoArgumentCaptor.capture());
                Notificacao notificacao = notificacaoArgumentCaptor.getValue();
                assertEquals("Novo post criado -> " + post.getTitulo(), notificacao.getConteudo());
            }

        }

        @Nested
        @DisplayName("Quando editar")
        class QuandoEditar {

            @Spy
            Post post = PostTestData.umPostExistente().build();

            @BeforeEach
            void beforeEach() {
                when(armazenamentoPost.encontrarPorId(post.getId())).thenAnswer(invocation -> Optional.ofNullable(post));
                when(armazenamentoPost.salvar(any(Post.class))).thenAnswer(invocation -> {
                    Post postPassado = invocation.getArgument(0, Post.class);
                    return postPassado;
                });
            }

            @Test
            @DisplayName("Então deve salvar")
            void entaoDeveSalvar() {
                cadastroPost.editar(post);
                verify(armazenamentoPost, times(1)).salvar(post);
            }

            @Test
            @DisplayName("Então deve retornar o mesmo ID")
            void entaoDeveRetornarOMesmoId() {
                Post postEditado = cadastroPost.editar(post);
                assertEquals(post.getId(), postEditado.getId());
            }

            @Test
            @DisplayName("Então deve atualizar dados do post")
            void entaoDeveAtualizarDadosDoPost() {
                cadastroPost.editar(post);
                verify(post, times(1)).atualizarComDados(post);
            }

            @Test
            @DisplayName("Então deve atualizar antes de salvar")
            void entaoDeveAtualizarAntesDeSalvar() {
                cadastroPost.editar(post);

                InOrder inOrder = inOrder(post, armazenamentoPost);
                inOrder.verify(post, times(1)).atualizarComDados(post);
                inOrder.verify(armazenamentoPost, times(1)).salvar(post);
            }

            @Test
            @DisplayName("Então deve calcular os ganhos")
            void entaoDeveCalcularOsGanhos() {
                cadastroPost.editar(post);
                verify(calculadoraGanhos, times(1)).calcular(post);
            }

            @Test
            @DisplayName("Então deve calcular os ganhos antes de salvar")
            void entaoDeveCalcularOsGanhosAntesDeSalvar() {
                cadastroPost.editar(post);

                InOrder inOrder = inOrder(calculadoraGanhos, armazenamentoPost);
                inOrder.verify(calculadoraGanhos, times(1)).calcular(post);
                inOrder.verify(armazenamentoPost, times(1)).salvar(post);
            }

        }

        @Nested
        @DisplayName("Quando remover")
        class QuandoRemover {

            @Spy
            Post post = PostTestData.umPostExistente().build();

            @BeforeEach
            void beforeEach() {
                when(armazenamentoPost.encontrarPorId(post.getId())).thenAnswer(invocation -> Optional.ofNullable(post));
            }

            @Test
            @DisplayName("Então deve remover")
            void entaoDeveRemover() {
                cadastroPost.remover(post.getId());
                verify(armazenamentoPost, times(1)).remover(post.getId());
            }

        }

    }

    @Nested
    @DisplayName("Dado um post null")
    class DadoUmPostNull {

        @Nested
        @DisplayName("Quando criar")
        class QuandoCriar {

            @Test
            @DisplayName("Então deve lançar uma exception e não criar")
            void entaoDeveLancarUmaExceptionENaoCriar() {
                assertThrows(NullPointerException.class, () -> cadastroPost.criar(null));
                verify(armazenamentoPost, never()).salvar(any(Post.class));
            }

        }

        @Nested
        @DisplayName("Quando editar")
        class QuandoEditar {

            @Test
            @DisplayName("Então deve lançar exception e não editar")
            void entaoDeveLancarExceptionENaoEditar() {
                assertThrows(NullPointerException.class, () -> cadastroPost.editar(null));
                verify(armazenamentoPost, never()).salvar(any(Post.class));
            }

        }

        @Nested
        @DisplayName("Quando remover")
        class QuandoRemover {

            @Test
            @DisplayName("Então deve lançar exception e não remover")
            void entaoDeveLancarExceptionENaoRemover() {
                assertThrows(NullPointerException.class, () -> cadastroPost.remover(null));
                verify(armazenamentoPost, never()).remover(anyLong());
            }

        }

    }

    @Nested
    @DisplayName("Dado um post válido e pago")
    class DadoUmaEdicaoComPostValidoEPago {

        @Spy
        Post post = PostTestData.umPostExistentePago().build();

        @BeforeEach
        void beforeEach() {
            when(armazenamentoPost.encontrarPorId(post.getId())).thenAnswer(invocation -> Optional.ofNullable(post));
        }

        @Nested
        @DisplayName("Quando editar")
        class QuandoEditar {
            @Test
            @DisplayName("Então não deve recalcular os ganhos")
            void entaoNaoDeveRecalcularOsGanhos() {
                when(armazenamentoPost.salvar(any(Post.class))).thenAnswer(invocation -> {
                    Post postPassado = invocation.getArgument(0, Post.class);
                    return postPassado;
                });

                cadastroPost.editar(post);
                verify(calculadoraGanhos, never()).calcular(post);
            }

        }

        @Nested
        @DisplayName("Quando remover")
        class QuandoRemover {

            @Test
            @DisplayName("Então deve lançar exception e não remover")
            void entaoDeveLancarExceptionENaoRemover() {
                assertThrows(RegraNegocioException.class, () -> cadastroPost.remover(post.getId()));
                verify(armazenamentoPost, never()).remover(post.getId());
            }

        }

    }

    @Nested
    @DisplayName("Dado um post inexistente")
    class DadoUmaEdicaoDeUmPostInexistente {

        @Spy
        Post post = PostTestData.umPostExistente().build();

        @BeforeEach
        void beforeEach() {
            when(armazenamentoPost.encontrarPorId(post.getId())).thenAnswer(invocation -> Optional.empty());
        }

        @Nested
        @DisplayName("Quando editar")
        class QuandoEditar {

            @Test
            @DisplayName("Então deve lançar exception e não salvar")
            void entaoDeveLancarExceptionENaoSalvar() {
                assertThrows(PostNaoEncontradoException.class, () -> cadastroPost.editar(post));
                verify(armazenamentoPost, never()).salvar(any(Post.class));
            }

        }

        @Nested
        @DisplayName("Quando remover")
        class QuandoRemover {

            @Test
            @DisplayName("Então deve lançar exception e não remover")
            void entaoDeveLancarExceptionENaoRemover() {
                assertThrows(PostNaoEncontradoException.class, () -> cadastroPost.remover(post.getId()));
                verify(armazenamentoPost, never()).remover(post.getId());
            }

        }

    }

    @Nested
    @DisplayName("Dado um post válido e publicado")
    class DadoUmPostValidoNaoPagoEPublicado {

        @Nested
        @DisplayName("Quando remover")
        class QuandoRemover {

            @Spy
            Post post = PostTestData.umPostExistentePublicado().build();

            @BeforeEach
            void beforeEach() {
                when(armazenamentoPost.encontrarPorId(post.getId())).thenAnswer(invocation -> Optional.of(post));
            }

            @Test
            @DisplayName("Então deve lançar exception e não remover")
            void entaoDeveLancarExceptionENaoRemover() {
                assertThrows(RegraNegocioException.class, () -> cadastroPost.remover(post.getId()));
                verify(armazenamentoPost, never()).remover(post.getId());
            }

        }

    }

}