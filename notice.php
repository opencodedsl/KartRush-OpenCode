<?php

namespace kartrush\api;

class notice{

    static $category = [];
    public static function api_(
        $url = "",
        $api = "",
        $category = ""
    ): array
    {
        $ch = curl_init();

        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [ 'Content-Type: application/json', 'x-nxopen-api-key: ' . $api]);
        $response = curl_exec($ch);

        curl_close($ch);

        $data = json_decode($response, true);
        self::$category[$category] = $category;
        return $data;
    }
    /**
     *	모든 게시글을 불러옵니다.
     */

    public static function getList(
        $category = "",
        int $page = 1
    ): string|array {
        require_once 'apikey.php';
        $api = "api key";
        $base = "https://open.api.nexon.com/kartrush/v1/";
        $urls = [[
            "url" => $base . "notice",
            "api" => $api,
            "type" => "notice"
        ], [
            "url" => $base . "notice-season",
            "api" => $api,
            "type" => "season"
        ], [
            "url" => $base . "notice-event",
            "api" => $api,
            "type" => "event"
        ]];
        $multi = curl_multi_init();
        $handles = [];
        foreach ($urls as $key => $item) {
            $ch = curl_init();
            curl_setopt_array($ch, [
                CURLOPT_URL => $item["url"],
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_HTTPHEADER => [
                    'Content-Type: application/json',
                    'x-nxopen-api-key: ' . $item["api"]
                ],
                CURLOPT_CONNECTTIMEOUT => 3,
                CURLOPT_TIMEOUT => 5,
                CURLOPT_ENCODING => '',]);
            curl_multi_add_handle($multi, $ch);
            $handles[$key] = $ch;
        }
        $running = null;
        do {
            curl_multi_exec($multi, $running);
            if ($running) {
                curl_multi_select($multi, 1);
            }
        } while ($running);
        $results = [];
        foreach ($handles as $key => $ch) {
            $response = curl_multi_getcontent($ch);
            $results[$urls[$key]["type"]] = json_decode($response, true) ?? [];
            curl_multi_remove_handle($multi, $ch);
            curl_close($ch);
        }
        curl_multi_close($multi);
        $notice = $results["notice"]["notice"] ?? [];
        $notice_season = $results["season"]["season_notice"] ?? [];
        $event_notice = $results["event"]["event_notice"] ?? [];
        $note = array_merge($notice, $notice_season, $event_notice, $gm);
        usort($note, function ($a, $b) {return strtotime($b['date']) <=> strtotime($a['date']);});
        $perPage = 5;
        $page = max(1, $page);
        $offset = ($page - 1) * $perPage;
        return array_slice(
            $note,
            $offset,
            $perPage
        );
    }
    /**
     * 특정 게시글을 불러옵니다.
     */
    public static function NoteDetails(int|string $post): array{
        $url = "https://forum.nexon.com/api/v1/thread/" . $post;
        $ch = curl_init();

        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
        $response = curl_exec($ch);
        curl_close($ch);
        $data = json_decode($response, true);
        $post_id = $data["threadId"] ?? "";
        $title = $data["title"] ?? "";
        $content = $data["content"] ?? "";
        if ($post_id !== "" && $title !== "" && $content !== ""){
            $info = [
                "post_id" => $post_id,
                "title" => $title,
                "content" => $content,
                "code" => 200,
                "message" => "응답 성공!"
            ];
        }else{
            $info = [
                "code" => 404,
                "message" => "게시글을 찾을 수 없습니다."
            ];
        }
        return $info;
    }
}