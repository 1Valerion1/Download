import {Pie} from '@ant-design/plots';
import {PieChartProps} from "./PieChart.types.ts";

export const PieChart: React.FC<PieChartProps> = ({data}) => {
    const config = {
        appendPadding: 10,
        data,
        angleField: 'value',
        colorField: 'type',
        radius: 0.75,
        label: {
            type: 'spider',
            labelHeight: 28,
            content: '{name}\n{percentage}',
        },
        interactions: [
            {
                type: 'element-selected',
            },
            {
                type: 'element-active',
            },
        ],
    };

    return <Pie {...config} style={{width: 700}}/>;
};
