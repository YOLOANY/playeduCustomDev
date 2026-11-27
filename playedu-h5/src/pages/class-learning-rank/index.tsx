import { useEffect, useRef, useState } from "react";
import { Image, Skeleton, Tabs } from "antd-mobile";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { user } from "../../api";
import { Empty } from "../../components";
import backIcon from "../../assets/images/commen/icon-back.png";
import styles from "./index.module.scss";

const ranges: { key: ClassLearningRankRange; title: string }[] = [
  { key: "today", title: "今日" },
  { key: "week", title: "本周" },
  { key: "month", title: "本月" },
  { key: "all", title: "累计" },
];

const formatDuration = (duration: number) => {
  const totalMinutes = Math.floor(duration / 60000);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`;
  }
  if (minutes > 0) {
    return `${minutes}分钟`;
  }
  return duration > 0 ? "不足1分钟" : "0分钟";
};

const ClassLearningRankPage = () => {
  const navigate = useNavigate();
  const currentDepId = useSelector(
    (state: any) => state.loginUser.value.currentDepId
  );
  const [range, setRange] = useState<ClassLearningRankRange>("today");
  const [ranking, setRanking] = useState<ClassLearningRankItemModel[]>([]);
  const [loading, setLoading] = useState(false);
  const requestRef = useRef(0);

  useEffect(() => {
    document.title = "班级排行";
  }, []);

  useEffect(() => {
    if (currentDepId === 0) {
      requestRef.current += 1;
      setRanking([]);
      setLoading(false);
      return;
    }

    const requestId = ++requestRef.current;
    setLoading(true);
    setRanking([]);
    user
      .classLearningRank(currentDepId, range)
      .then((res: any) => {
        if (requestId !== requestRef.current) {
          return;
        }
        const data = res.data as ClassLearningRankModel;
        setRanking(data.data || []);
      })
      .catch(() => {})
      .finally(() => {
        if (requestId === requestRef.current) {
          setLoading(false);
        }
      });

    return () => {
      if (requestRef.current === requestId) {
        requestRef.current += 1;
      }
    };
  }, [currentDepId, range]);

  const currentClass = ranking.find((item) => item.is_current);

  return (
    <div className={`main-body ${styles["page"]}`}>
      <div className={`main-header ${styles["sticky-header"]}`}>
        <Image
          className="back-icon"
          src={backIcon}
          onClick={() => navigate(-1)}
        />
        <div className="main-title">班级排行</div>
      </div>
      <div className={styles["range-tabs"]}>
        <Tabs
          activeKey={range}
          onChange={(key) => setRange(key as ClassLearningRankRange)}
          style={{
            "--fixed-active-line-width": "24px",
            "--active-line-height": "3px",
            "--active-title-color": "#ff4d4f",
            "--active-line-color": "#ff4d4f",
            "--title-font-size": "15px",
          }}
        >
          {ranges.map((item) => (
            <Tabs.Tab title={item.title} key={item.key} />
          ))}
        </Tabs>
      </div>
      <div className={styles["content"]}>
        {currentClass && (
          <div className={styles["summary"]}>
            <div className={styles["summary-card"]}>
              <div className={styles["summary-label"]}>当前班级排名</div>
              <div className={styles["summary-value"]}>
                第{currentClass.rank}名
              </div>
              <div className={styles["summary-name"]}>
                {currentClass.dep_name}
              </div>
            </div>
            <div className={styles["summary-card"]}>
              <div className={styles["summary-label"]}>当前班级人均</div>
              <div className={styles["summary-duration"]}>
                {formatDuration(currentClass.average_duration)}
              </div>
              <div className={styles["summary-name"]}>
                共{currentClass.student_count}名学员
              </div>
            </div>
          </div>
        )}
        {currentDepId === 0 && (
          <div className={styles["no-department"]}>
            请先在“我的”页面选择班级
          </div>
        )}
        {currentDepId !== 0 && loading && (
          <div className={styles["loading-box"]}>
            <Skeleton animated style={{ width: "55%", height: 32 }} />
            <Skeleton animated style={{ width: "100%", height: 96 }} />
            <Skeleton animated style={{ width: "100%", height: 96 }} />
            <Skeleton animated style={{ width: "100%", height: 96 }} />
          </div>
        )}
        {currentDepId !== 0 && !loading && ranking.length === 0 && <Empty />}
        {!loading && ranking.length > 0 && (
          <>
            <div className={styles["section-title"]}>按人均学习时长排名</div>
            <div className={styles["rank-list"]}>
              {ranking.map((item) => (
                <div
                  className={`${styles["rank-item"]} ${
                    item.is_current ? styles["current-item"] : ""
                  }`}
                  key={item.dep_id}
                >
                  <div
                    className={`${styles["rank-number"]} ${
                      item.rank <= 3 ? styles["top-rank"] : ""
                    }`}
                  >
                    {item.rank}
                  </div>
                  <div className={styles["class-info"]}>
                    <div className={styles["class-name-row"]}>
                      <div className={styles["class-name"]}>{item.dep_name}</div>
                      {item.is_current && (
                        <div className={styles["current-tag"]}>当前班级</div>
                      )}
                    </div>
                    <div className={styles["class-detail"]}>
                      {item.student_count}名学员 · 累计
                      {formatDuration(item.total_duration)}
                    </div>
                  </div>
                  <div className={styles["average-box"]}>
                    <div className={styles["average-value"]}>
                      {formatDuration(item.average_duration)}
                    </div>
                    <div className={styles["average-label"]}>人均学习</div>
                  </div>
                </div>
              ))}
            </div>
            <div className={styles["list-end"]}>共{ranking.length}个班级</div>
          </>
        )}
      </div>
    </div>
  );
};

export default ClassLearningRankPage;
