package com.algaworks.junit.utilidade;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FiltroNumerosTest {

    // Given, When, Then
    // Dado, Quando, Então
    @Test
    public void Dado_um_lista_de_numero_Quando_filtrar_por_pares_Entao_deve_retornar_apenas_numeros_pares() {
        List<Integer> numeros = Arrays.asList(1, 2, 3, 4);
        List<Integer> numerosParesEsperados = Arrays.asList(2, 4);
        List<Integer> resultadoFiltro = FiltroNumeros.numerosPares(numeros);

        assertIterableEquals(numerosParesEsperados, resultadoFiltro);
    }

    @Test
    public void Dado_um_lista_de_numero_Quando_filtrar_por_pares_Entao_deve_retornar_apenas_numeros_impares() {
        List<Integer> numeros = Arrays.asList(1, 2, 3, 4);
        List<Integer> numerosImparesEsperados = Arrays.asList(1, 3);
        List<Integer> resultadoFiltro = FiltroNumeros.numerosImpares(numeros);

        assertIterableEquals(numerosImparesEsperados, resultadoFiltro);
    }

}