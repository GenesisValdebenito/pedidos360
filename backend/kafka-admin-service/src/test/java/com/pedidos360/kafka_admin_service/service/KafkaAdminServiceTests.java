package com.pedidos360.kafka_admin_service.service;

import com.pedidos360.kafka_admin_service.dto.TopicoConfigRequest;
import com.pedidos360.kafka_admin_service.dto.TopicoRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaAdminServiceTests {

    @Mock
    private AdminClient adminClient;

    @Mock
    private CreateTopicsResult createTopicsResult;

    @Mock
    private ListTopicsResult listTopicsResult;

    @Mock
    private DeleteTopicsResult deleteTopicsResult;

    @Mock
    private KafkaFuture<Void> voidFuture;

    @Mock
    private KafkaFuture<Set<String>> namesFuture;

    private KafkaAdminService service;

    @BeforeEach
    void configurar() {
        service = new KafkaAdminService(adminClient);
    }

    @Test
    void crearTopicoLlamaAdminClient() throws Exception {
        when(adminClient.createTopics(anyList())).thenReturn(createTopicsResult);
        when(createTopicsResult.all()).thenReturn(voidFuture);
        when(voidFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(null);

        service.crearTopico(new TopicoRequest(
                "pedidos.eventos", 3, (short) 3, 604800000L, "delete", 2));

        verify(adminClient).createTopics(anyList());
    }

    @Test
    void listarTopicosDevuelveNombresOrdenados() throws Exception {
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(namesFuture);
        when(namesFuture.get(anyLong(), any(TimeUnit.class)))
                .thenReturn(Set.of("stock.eventos", "pedidos.eventos"));

        assertEquals(List.of("pedidos.eventos", "stock.eventos"), service.listarTopicos());
    }

    @Test
    void eliminarTopicoLlamaAdminClient() throws Exception {
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(namesFuture);
        when(namesFuture.get(anyLong(), any(TimeUnit.class)))
                .thenReturn(Set.of("pedidos.eventos"));
        when(adminClient.deleteTopics(List.of("pedidos.eventos"))).thenReturn(deleteTopicsResult);
        when(deleteTopicsResult.all()).thenReturn(voidFuture);
        when(voidFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(null);

        service.eliminarTopico("pedidos.eventos");

        verify(adminClient).deleteTopics(List.of("pedidos.eventos"));
    }

    @Test
    void modificarConfiguracionSinCambiosFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> service.modificarConfiguracion("pedidos.eventos",
                        new TopicoConfigRequest(null, null)));

        verifyNoInteractions(adminClient);
        verify(adminClient, never()).incrementalAlterConfigs(any());
    }
}
