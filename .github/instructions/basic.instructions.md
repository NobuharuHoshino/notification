---
applyTo: '**'
---
Provide project context and coding guidelines that AI should follow when generating code, answering questions, or reviewing changes.

# AIエージェント向けコマンド命令（PJ標準）

- **PJ**：プロジェクト全体を読み込み、最新状態・構成・依存関係・設計方針を把握すること。その後、必ず `./gradlew clean build` でクリーン＆ビルドし、ビルド結果を報告すること。
- **TEST**：`./gradlew clean test jacocoTestReport` でクリーン＆ビルド＋Jacocoカバレッジレポート付きテストを実行し、結果を報告すること。
- **PUSH&COMMIT [コメント] [プッシュ先]**：指定コメントでコミットし、指定のリモートブランチへプッシュすること。git commit -am "[コメント]" / git push origin [プッシュ先]

（例）
PUSH&COMMIT "バグ修正" origin/feature/new-create

---
PJ標準コマンドの詳細運用ルール。AIは上記コマンド指示があった場合、必ずこの手順・命令に従うこと。