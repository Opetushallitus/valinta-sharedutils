package fi.vm.sade.valinta.sharedutils;

import fi.vm.sade.auditlog.Operation;

public enum ValintaperusteetOperation implements Operation{
    HAKEMUS_TILAMUUTOS,
    HAKEMUS_TILAMUUTOS_ERILLISHAKU,
    VALINNANVAIHE_TUONTI_KAYTTOLIITTYMA,
    ERILLISHAKU_TUONTI_HAKIJA_POISTO,
    ERILLISHAKU_TUONTI_HAKIJA_PAIVITYS,
    PISTETIEDOT_TUONTI_EXCEL,
    PISTETIEDOT_KAYTTOLIITTYMA,
    PISTETIEDOT_AMMATTILLISEN_KIELIKOKEEN_MIGRAATIO,
    VALINNANVAIHE_TUONTI_EXCEL,
    HAKIJARYHMA_POISTO,
    HAKIJARYHMA_SIIRTO,
    HAKIJARYHMA_PAIVITYS,
    HAKIJARYHMA_JARJESTA,
    VALINTATAPAJONO_HAKIJARYHMAT_JARJESTA,
    HAKIJARYHMA_VALINTATAPAJONO_LIITOS_POISTO,
    HAKIJARYHMA_VALINTATAPAJONO_LIITOS_PAIVITYS,
    HAKU_TUONNIN_AKTIVOINTI,
    HAKUKOHDE_TUONNIN_AKTIVOINTI,
    HAKUKOHDE_LISAYS_VALINTARYHMA,
    HAKUKOHDE_LISAYS_HAKIJARYHMA,
    HAKUKOHDE_LISAYS_HAKUKOHDEKOODI,
    HAKUKOHDE_LISAYS_VALINNANVAIHE,
    HAKUKOHDE_LIITOS_HAKIJARYHMA,
    HAKUKOHDE_SIIRTO_VALINTARYHMAAN,
    HAKUKOHDE_PAIVITYS,
    HAKUKOHDE_HAKUKOHDEKOODI_PAIVITYS,
    JARJESTYSKRITEERI_POISTO,
    JARJESTYSKRITEERIT_JARJESTA,
    JARJESTYSKRITEERI_PAIVITYS,
    KELA_VASTAANOTTO_EXPORT_LATAUS_FTP,
    KELA_VASTAANOTTO_EXPORT_LUONTI,
    LASKENTAKAAVA_LISAYS,
    LASKENTAKAAVA_POISTO,
    LASKENTAKAAVA_SIIRTO,
    LASKENTAKAAVA_PAIVITYS,
    VALINNANVAIHE_LISAYS_VALINTATAPAJONO,
    VALINNANVAIHE_LISAYS_VALINTAKOE,
    VALINNANVAIHE_POISTO,
    VALINNANVAIHE_JARJESTA,
    VALINNANVAIHE_PAIVITYS,
    VALINTAKOE_POISTO,
    VALINTAKOE_PAIVITYS,
    AUTOMAATTISEN_SIJOITTELUN_SIIRRON_PAIVITYS,
    LAPSIVALINTARYHMA_LISAYS,
    VALINTARYHMA_POISTO,
    VALINTARYHMA_LISAYS,
    LAPSIVALINTARYHMA_LISAYS_PARENT,
    VALINTARYHMA_LISAYS_HAKIJARYHMA,
    VALINTARYHMA_LISAYS_HAKUKOHDEKOODI,
    VALINTARYHMA_LISAYS_VALINNANVAIHE,
    VALINTARYHMA_LISAYS_VALINTAKOEKOODI,
    VALINTARYHMA_PAIVITYS,
    VALINTARYHMA_PAIVITYS_HAKUKOHDEKOODI,
    VALINTARYHMA_PAIVITYS_VALINTAKOODI,
    VALINTATAPAJONO_POISTO,
    VALINTATAPAJONO_LISAYS_HAKIJARYHMA,
    VALINTATAPAJONO_LISAYS_JARJESTYSKRITEERI,
    VALINTATAPAJONO_JARJESTA,
    VALINTATAPAJONO_LIITOS_HAKIJARYHMA,
    VALINTATAPAJONO_PAIVITYS,
    VALINNANVAIHEEN_HAKEMUKSET_HAKU,
    SIJOITTELU_KAYNNISTYS;
    //
    //HAKIJARYHMA_VALINTATAPAJONO_JARJESTAMINEN;
}
