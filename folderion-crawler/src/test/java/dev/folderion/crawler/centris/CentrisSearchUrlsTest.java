package dev.folderion.crawler.centris;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CentrisSearchUrlsTest {

    private static final String Q =
            "H4sIAAAAAAAACo2UQW-bQBCF_wrilEquRFy7jptT7DZOVCe1QhUpbXMYwxhGXVh3d3GCovz3DiY2eI3a5WJ2-ebt8N7gFz8T2v_kB37PXyr5G9VUxsgbvJarFUX4Fct6WWicoUwUrNMyTGGNXBf0fF3d3hM-8fLnI68RVJTeQlap3ADlXqTIoCJgjRUJvq3IFz8DE6Xfy3XFTcmUn0kbRZFhzOCz4d2Qq837OWRLVMY7uZG5-VUEwZexqn8SwndMU8zsaDD0X3suqhNZRCmqDQmBTfXYtVpJrUHFTeXAsXIO3kIBKdqfeuZ86lzmSYEFCe-kNuWqqDxpvf3oSGkXFkUXCqHRqmx8MxBY71oLyOO90iychYOP_dN_NnaLlKRLWahUyjjcBt7oHzz0vhn2Wvc827cf5_9z7kKITgN431tKJYsk1fu2WYpnb0UoYn0PosB6xrYb1_Gxr5uKaY2NA1hn5QAO3MAzd8WRBR5Fu0MP0usW7s5uJ9DE0l1dh7KjbXKh5JrHstym2WAh5YnAS8hIlFeS_xgsfTCYSFW2Ku5QU4y5IRAWHKIQLGefAPwtH4J3Uma6hXyoRr4_sagJmHQhn2JUNt_v5GegIME2d9rJzWnDXVrphH8KUHiJaCz6gWOYypwtLiJDMm_Lj8eBOz1uXXZH_JW79lPZueC42xYPhwFfDuDoDXx8_Qtf-nEAXQYAAA";

    private static final String BASE =
            "https://www.centris.ca/en/houses~for-sale?q=" + Q
                    + "&v=2&sortSeed=936090035&sort=DateDesc&pageSize=20&page=3";

    @Test
    void readsPageAndPageSize() {
        assertEquals(3, CentrisSearchUrls.page(BASE));
        assertEquals(20, CentrisSearchUrls.pageSize(BASE));
    }

    @Test
    void rewritesPageWithoutTouchingEncodedQ() {
        String page1 = CentrisSearchUrls.withPage(BASE, 1);
        assertEquals(1, CentrisSearchUrls.page(page1));
        assertEquals(20, CentrisSearchUrls.pageSize(page1));
        assertTrue(page1.contains("q=" + Q));
        assertTrue(page1.contains("sort=DateDesc"));
        assertTrue(page1.contains("pageSize=20"));
        assertTrue(page1.contains("page=1"));

        String page5 = CentrisSearchUrls.withPage(page1, 5);
        assertEquals(5, CentrisSearchUrls.page(page5));
        assertTrue(page5.contains("q=" + Q));
    }
}
