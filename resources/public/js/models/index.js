$('.confirmable').click(function() { return confirm('Are you sure?') })
var data = [[1.0,1.0,"NORM_300",0.9259436219077685,"MATCH_NORM.DIST_300",999999.0,"A","DISTRIBUTION",11.0,"A","Norm.Dist_300"],[1.0,2.0,"NORM_300",150.0,"MATCH_NORM.DIST_300",999999.0,"B","DISTRIBUTION",11.0,"B","Norm.Dist_300"],[1.0,3.0,"NORM_80",1.2569792480495925,"PART1_NORM.DIST_80",999999.0,"A","DISTRIBUTION",21.0,"A","Norm.Dist_80"],[1.0,4.0,"NORM_80",3.5156562291956805,"PART1_NORM.DIST_80",999999.0,"B","DISTRIBUTION",21.0,"B","Norm.Dist_80"],[1.0,5.0,"POISSON_80",0.9259259259259269,"PART2_POISSON_80",999999.0,"A","DISTRIBUTION",31.0,"A","Poisson_80"],[1.0,6.0,"POISSON_80",150.0,"PART2_POISSON_80",999999.0,"B","DISTRIBUTION",31.0,"B","Poisson_80"],[1.0,7.0,"POISSON_300",0.9259259259259339,"PART3_POISSON_300",999999.0,"A","DISTRIBUTION",41.0,"A","Poisson_300"],[1.0,8.0,"POISSON_300",150.0,"PART3_POISSON_300",999999.0,"B","DISTRIBUTION",41.0,"B","Poisson_300"],[100.0,9.0,"VPR_NORM_150",1.4467592592592593,"MATCH_VPR_NORM.DIST_300",50.0,"A","OTHER",12.0,"A","ВПР Norm.Dist_81"],[100.0,10.0,"VPR_NORM_150",2.572016460905349,"MATCH_VPR_NORM.DIST_300",50.0,"B","OTHER",12.0,"B","ВПР Norm.Dist_81"],[100.0,11.0,"VPR_POISSON_81",1.2683916793505834,"MATCH_VPR_POISSON_81",50.0,"A","OTHER",13.0,"A","ВПР Poisson_81"],[100.0,12.0,"VPR_POISSON_81",3.4293552812071324,"MATCH_VPR_POISSON_81",50.0,"B","OTHER",13.0,"B","ВПР Poisson_81"],[100.0,13.0,"PROB",1.3888888888888788,"MATCH_PROB",999999.0,"A","OTHER",14.0,"A","Match Probability"],[100.0,14.0,"PROB",2.777777777777817,"MATCH_PROB",999999.0,"B","OTHER",14.0,"B","Match Probability"],[2.0,15.0,"TOTAL",3.0369200002774592,"MATCH_TOTAL",208.5,"OVER","SIMPLE OPERATION",15.0,"Over","Match Total"],[2.0,16.0,"TOTAL",1.3320563034188828,"MATCH_TOTAL",208.5,"UNDER","SIMPLE OPERATION",15.0,"Under","Match Total"],[2.0,17.0,"TOTAL",1.889017399417632,"MATCH_TOTAL",88.5,"OVER","SIMPLE OPERATION",15.0,"Over","Match Total"],[2.0,18.0,"TOTAL",1.816120516885687,"MATCH_TOTAL",88.5,"UNDER","SIMPLE OPERATION",15.0,"Under","Match Total"],[2.0,19.0,"TOTAL",1.932746209988429,"MATCH_TOTAL",89.5,"OVER","SIMPLE OPERATION",15.0,"Over","Match Total"],[2.0,20.0,"TOTAL",1.7774570619916745,"MATCH_TOTAL",89.5,"UNDER","SIMPLE OPERATION",15.0,"Under","Match Total"]];

$(function() {
   $('.sheet-table').each(function(i, el) {
       window.ht = new Handsontable(el, {
         data: data,
         rowHeaders: true,
         colHeaders: true,
         allowInsertColumn: false,
         allowInsertRow: false,
         allowRemoveColumn: false,
         allowRemoveRow: false,
         multiSelect: false,
         copyable: false,
         readOnly: true,
         colWidths: 100
       })
       ht.addHook("afterSelection", function(r, c, r2, c2) {
           if (c >= 0) {
              console.log(ht.getColHeader(c), r+1);
           }
       })
   })
})